package n2lf.wirelesscontroller.service;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.content.Context;
import android.os.Message;
import n2lf.wirelesscontroller.utilities.Utilities;
import java.net.Socket;
import java.io.BufferedWriter;
import java.io.IOException;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.ComponentName;
import java.io.OutputStreamWriter;

public class SocketClientService extends Service
{
    static final int ACTION_SENDER_ERROR = 128;
    static final int ACTION_SENDER_SUCCESS = 127;
    private MessageHandler messageHandler;
    private ActionSender actionSender;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(messageHandler == null){
            messageHandler = new MessageHandler(this);
        }
        actionSender = new ActionSender(messageHandler ,intent.getStringExtra("ip"), intent.getIntExtra("port" , Utilities.DefaultPort) , intent.getStringExtra("modelName"));
        actionSender.startAndGetDialog().show();
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public IBinder onBind(Intent p1){
        return null;
    }

    public ActionSender getActionSender(){
        return actionSender;
    }

    public class ActionSender extends Thread//一一对应原则，一个model一个对象
    {
        private final SyncedLinkedList actionQueue;
        private final Handler handler;
        private final String ip;
        private final int port;
        private boolean isStopped;
        private boolean isBinded;
        private final ProgressDialog progressDialog;
        private final ServiceConnection connection;
        private final String modelName;
        private OverlayService overlayService;

        ActionSender(Handler handler , String ip , int port , String modelName){
            this.handler = handler;
            this.ip = ip;
            this.port = port;
            this.isStopped = false;
            this.isBinded = false;
            this.modelName = modelName;
            actionQueue = new SyncedLinkedList();
            progressDialog = new ProgressDialog(getApplicationContext());
            progressDialog.setTitle("请等待");
            progressDialog.setMessage("连接中...");
            progressDialog.setCancelable(false);
            progressDialog.setButton("取消" , 
                new android.content.DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface p1, int p2){
                        progressDialog.dismiss();
                        ActionSender.this.isStopped = true;
                    }
                });
            progressDialog.getWindow().setType(Utilities.getLayoutParamsType());
            connection = new ServiceConnection(){
                @Override
                public void onServiceConnected(ComponentName p1, IBinder p2){//onBind结束才会执行此方法
                    ((OverlayService.OSBinder)p2).setBindedService(SocketClientService.this);
                    ((OverlayService.OSBinder)p2).setSyncedLinkedList(actionQueue);
                    overlayService = ((OverlayService.OSBinder)p2).getOverlayService();
                    overlayService.loadOverlay();
                }
                @Override
                public void onServiceDisconnected(ComponentName p1){

                }};
        }

        @Override
        public void run(){
            try{
                // System.out.println("ip:"+ip+",port:"+port+",ModelName:"+modelName);
                Socket socket = new Socket(ip, port);
                OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
                BufferedWriter bw = new BufferedWriter(osw);
                progressDialog.dismiss();//Success
                Intent intent = new Intent(getApplicationContext() , OverlayService.class);
                intent.putExtra("modelName" , modelName);
                if(!isBinded){
                    bindService(intent ,connection , Context.BIND_AUTO_CREATE);
                }
                isBinded = true;
                while(true){
                    if(isStopped){
                        break;}
                    if(actionQueue.isEmpty()){
                        try{
                            Thread.sleep(Utilities.ThreadSleepTime);}
                        catch (InterruptedException e){
                            e.printStackTrace();}
                    }else{
                        //System.out.println(actionQueue.getLast());
                        bw.write(actionQueue.getAndRemoveLast());
                        bw.newLine();
                        bw.flush();
                    }
                }
                bw.close();
                osw.close();
                socket.close();
                overlayService.stopOverlay(true);
                unbindService(connection);
                isBinded = false;
                stopSelf();
            }
            catch (IOException e){
                progressDialog.dismiss();
                if(isBinded){
                    overlayService.stopOverlay(false);
                    unbindService(connection);
                    isBinded = false;
                } 
                if(isStopped){//是否意外停止，return为没有意外停止 比如ProgressDialog点击取消
                    return;}
                Message message = new Message();
                message.what = ACTION_SENDER_ERROR;
                message.obj = e;
                handler.sendMessage(message);
            }
        }

        public ProgressDialog startAndGetDialog(){
            super.start();
            return progressDialog;
        }

        public boolean isStopped(){
            return isStopped;
        }

        public void setToStop(){
            this.isStopped = true;
        }
    }

    private class MessageHandler extends Handler{
        Context context;
        MessageHandler(Context context){
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case ACTION_SENDER_ERROR:
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("错误");
                    builder.setMessage(msg.obj.toString());
                    builder.setCancelable(false);
                    builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(android.content.DialogInterface p1, int p2){
                                SocketClientService.this.stopSelf();
                            }
                        });
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setType(Utilities.getLayoutParamsType());
                    dialog.show();
                    return;
            }
        }
    }

    public class SyncedLinkedList extends java.util.LinkedList<String>{
        public synchronized String getAndRemoveLast(){
            String s = this.getLast();
            this.removeLast();
            return s;
        }

        @Override
        public synchronized void addFirst(String e){
            super.addFirst(e);
        }
    }
}
