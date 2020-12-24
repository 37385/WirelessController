package n2lf.wirelesscontroller.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.view.View.OnTouchListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import n2lf.wirelesscontroller.utilities.Utilities;
import android.widget.TextView;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.RelativeLayout;
import android.app.AlertDialog;
import n2lf.wirelesscontroller.R;
import android.content.DialogInterface;
import android.widget.ScrollView;
import android.view.LayoutInflater;
import android.widget.EditText;
import n2lf.wirelesscontroller.utilities.colorpicker.ColorPickerView;
import n2lf.wirelesscontroller.utilities.colorpicker.ColorUtil;
import android.text.Editable;
import android.text.TextWatcher;


public class ModelEditorActivity extends Activity
{
    private RelativeLayout relativeLayout;
    private TextView onEditTextView;
    private boolean isOnAddEvent = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.getActionBar().hide();
        this.setContentView(relativeLayout = new RelativeLayout(this));
        onEditTextView = new TextView(this);
        onEditTextView.setTextSize(Utilities.字体大小);
        onEditTextView.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams rLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rLP.addRule(RelativeLayout.CENTER_VERTICAL);
        relativeLayout.addView(onEditTextView,rLP);
        relativeLayout.post(new CreatFloatButton(this));//延时添加按钮，否则会throw
    }
    
    private boolean addButtonStarted(){
        isOnAddEvent = true;
        onEditTextView.setText(Utilities.点击选取位置[0]);//点击以选取按钮位置
        relativeLayout.setBackgroundColor(Utilities.EditorBackgroundColor);
        relativeLayout.setOnTouchListener(new onButtonAddTouchListener());
        return true;
    }
    
    private boolean addFinished(){
        isOnAddEvent = false;
        onEditTextView.setText("");
        relativeLayout.setBackgroundColor(Color.alpha(0));
        relativeLayout.setOnTouchListener(null);//it is ok
        return true;
    }
    
    
    private class onButtonAddTouchListener implements OnTouchListener
    {
        private int lastX;//你可以用float，但会出现按键漂移问题
        private int lastY;
        private float onDownX;
        private float onDownY;
        OverviewButton overviewButton;
        int buttonSize;
        @Override
        public boolean onTouch(View p1, MotionEvent event)
        {
            switch(event.getAction()){
                case event.ACTION_DOWN:
                    lastX = (int) (onDownX = event.getX());
                    lastY = (int) (onDownY = event.getY());
                    overviewButton = new OverviewButton(ModelEditorActivity.this);
           /*       tempButton.setHeight(Utilities.getDefaultButtonSize(ModelCreatorActivity.this));
                    tempButton.setWidth(Utilities.getDefaultButtonSize(ModelCreatorActivity.this));
                    这个不行*/
                    buttonSize = Utilities.getDefaultButtonSize(ModelEditorActivity.this);
                    overviewButton.setHeight(buttonSize);//用来保存大小，后面直接获取按钮大小
                    overviewButton.setWidth(buttonSize);
                    overviewButton.setX(onDownX-buttonSize/2);//否则按钮会在点击位置的右下角
                    overviewButton.setY(onDownY-buttonSize/2);    
                    relativeLayout.addView(overviewButton,buttonSize,buttonSize);
                    return true;
                case event.ACTION_MOVE:
                    overviewButton.setX(event.getX()-buttonSize/2);
                    overviewButton.setY(event.getY()-buttonSize/2);
                    return true;
                case event.ACTION_UP:
                    overviewButton.editThis();//must edit at once
                    return true;
                default:
                    return false;
            }
        }   
    }
    
    
    private class OverviewButton extends Button implements OnClickListener
    {
        private Context context;
        private AlertDialog.Builder buttonEditBuilder;//多次使用
        private AlertDialog.Builder tempBuilder;//每次setView()
        private ColorPickerView colorPickerView;//多次使用
        private int buttonColor;
        
        
        private Button textColorButton;
        private Button buttonColorButton;
        private Button buttonMappingButton;
        private EditText buttonHeightEditText;
        private EditText buttonWidthEditText;
        private EditText buttonNameEditText;
        private EditText buttonARGBEditText;
        private EditText stringARGBEditText;
        private TextView titleTextView;
        
        OverviewButton(Context context){
            super(context);
            this.context = context;
            //原生按钮
            this.setAllCaps(false);
            
            //颜色选择
            colorPickerView = new ColorPickerView(context);
            colorPickerView.setAlphaSliderVisible(true);//这个不能去掉，否则会出现error
            tempBuilder = new AlertDialog.Builder(context);
            buttonEditBuilder = new AlertDialog.Builder(context);
            
            //获取编辑dialog的控件
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_editor,null);
            
            (textColorButton = dialogView.findViewById(R.id.dialog_editor_button_textColor)).setOnClickListener(this);
            (buttonColorButton = dialogView.findViewById(R.id.dialog_editor_button_buttonColor)).setOnClickListener(this);
            (buttonMappingButton = dialogView.findViewById(R.id.dialog_editor_button_buttonMapping)).setOnClickListener(this);
            buttonHeightEditText = dialogView.findViewById(R.id.dialog_editor_editText_buttonHeight);
            buttonWidthEditText = dialogView.findViewById(R.id.dialog_editor_editText_buttonWidth);
            (buttonNameEditText = dialogView.findViewById(R.id.dialog_editor_editText_buttonName)).addTextChangedListener(new buttonNameEditTextWatcher(buttonNameEditText,this));
            (buttonARGBEditText = dialogView.findViewById(R.id.dialog_editor_editText_buttonARGB)).addTextChangedListener(new argbEditTextWatcher(buttonARGBEditText, new ColorPickerView.OnColorChangedListener(){
                                                                                                                      @Override
                                                                                                                      public void onColorChanged(int color){
                                                                                                                          OverviewButton.this.setButtonColor(color);
                                                                                                                      }
                                                                                                                  }));
            (stringARGBEditText = dialogView.findViewById(R.id.dialog_editor_editText_textARGB)).addTextChangedListener(new argbEditTextWatcher(stringARGBEditText, new ColorPickerView.OnColorChangedListener(){
                                                                                                                       @Override
                                                                                                                       public void onColorChanged(int color){
                                                                                                                           OverviewButton.this.setTextColor(color);
                                                                                                                       }
                                                                                                                   }));;
            titleTextView = dialogView.findViewById(R.id.dialog_editor_textView_title);
            
            ScrollView scrollView = new ScrollView(context);//使其可以上下滚动
            scrollView.addView(dialogView);
            buttonEditBuilder.setView(scrollView);
            buttonEditBuilder.setPositiveButton(Utilities.确定删除[0],//确定
                new android.content.DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface p1, int p2)
                    {
                        buttonEditBuilder.create().dismiss();
                    }
                });
            buttonEditBuilder.setNegativeButton(Utilities.确定删除[1],//删除
                new android.content.DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface p1, int p2)
                    {
                        relativeLayout.removeView(ModelEditorActivity.OverviewButton.this);
                    }
                });
        }
        
        
        @Override
        public void onClick(View p1)//按钮单击
        {
            if(p1==buttonColorButton){//设置颜色
                colorPickerView.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener(){
                        @Override
                        public void onColorChanged(int color){
                            buttonARGBEditText.setText(ColorUtil.convertToARGB(color).toString());
                            OverviewButton.this.setButtonColor(color);
                        }
                    });
                colorPickerView.setColor(buttonColor == 0 ?  Utilities.DefaultButtonColor : buttonColor);
                tempBuilder.setView(colorPickerView);
                tempBuilder.show();
            }else if(p1==textColorButton){
                colorPickerView.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener(){
                        @Override
                        public void onColorChanged(int color){
                            stringARGBEditText.setText(ColorUtil.convertToARGB(color).toString());
                            OverviewButton.this.setTextColor(color);
                        }
                    });
                colorPickerView.setColor(buttonColor == 0 ?  Utilities.DefaultStringColor : buttonColor);
                tempBuilder.setView(colorPickerView);
                tempBuilder.show();
            }else if(p1==buttonMappingButton){//设置按钮映射
                
            }
        }
     
        
        private void setButtonColor(int color)
        {
            buttonColor = color;
            this.getBackground().setAlpha(0);//按钮阴影便会消除
            //   this.setBackgroundColor(color);//不好看
            this.getBackground().setColorFilter(color , android.graphics.PorterDuff.Mode.SRC);
            //   this.getBackground().setTint(color); 会出现所有按钮颜色都改变的问题
            /**
             SRC SRC_IN  OK
             ADD 按钮不会透明
             SRC_OVER 同上
             SRC_ATOP 同上
             SCREEN 同上
             LIGHTEN 同上
             CLEAR 按钮只是透明
             SRC_OUT 同上
             DARKEN 是真的黑，没法用
             DST 同上
             OVERLAY 同上
             MULTIPLY 有透明，无颜色
             XOR 同上
            */
        }
        
        
        private float lastX;
        private float lastY;
        private float onDownX;
        private float onDownY;
        @Override
        public boolean onTouchEvent(MotionEvent event)//处理按钮移动
        {
            switch(event.getAction()){
                case event.ACTION_DOWN:
                    lastX = onDownX = event.getRawX();
                    lastY = onDownY = event.getRawY();
                    return true;
                case event.ACTION_MOVE:
                    this.setX(-(int)lastX+(int)event.getRawX()+(int)this.getX());//转换成int以防止出现过大的误差，导致按钮漂移
                    this.setY(-(int)lastY+(int)event.getRawY()+(int)this.getY());
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    return true;
                case event.ACTION_UP:
                    if((  Math.abs(onDownY-event.getRawY()) + Math.abs(onDownX-event.getRawX()) )< Utilities.偏移量){//判断是否点击按钮
                        this.editThis();//call edit dialog
                        return true;
                    }
                default:
                    return false;
            }
        }
        
        
        public void editThis(){
            buttonEditBuilder.create().show();
        }
        
        
        /**
        针对editText的事件处理，使不正确的文本更改为红色
        */
        
        private class argbEditTextWatcher implements android.text.TextWatcher{
            EditText editText;
            ColorPickerView.OnColorChangedListener onColorChangedListener;
            
            argbEditTextWatcher(EditText editText , ColorPickerView.OnColorChangedListener onColorChangedListener){
                this.editText = editText;
                this.onColorChangedListener = onColorChangedListener;
            }
            
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void afterTextChanged(Editable p1)
            {
                try
                {
                    onColorChangedListener.onColorChanged(ColorUtil.convertToColorInt(p1.toString()));
                    editText.setTextColor(Color.WHITE);
                }
                catch (Exception e)
                {
                    editText.setTextColor(Utilities.ErrorTextColor);
                }
            }
        }
        
        private class buttonSizeEditTextWatcher implements android.text.TextWatcher{
            EditText editText;
            OverviewButton button;
            buttonSizeEditTextWatcher(EditText editText , OverviewButton button){
                this.editText = editText;
                this.button = button;
            }
            
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void afterTextChanged(Editable p1)
            {
                
            }
        }
        
        private class buttonNameEditTextWatcher implements android.text.TextWatcher{
            EditText editText;
            OverviewButton button;
            buttonNameEditTextWatcher(EditText editText , OverviewButton button){
                this.editText = editText;
                this.button = button;
            }

            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4){
            }

            @Override
            public void afterTextChanged(Editable p1)
            {
                button.setText(p1);
            }
        }
    }
    
    
    
    private class CreatFloatButton extends Button implements Runnable , android.widget.PopupMenu.OnDismissListener ,android.widget.PopupMenu.OnMenuItemClickListener
    {
        Context context;
        WindowManager windowManager;
        WindowManager.LayoutParams layoutParams;
        PopupMenu popuMenu;
        CreatFloatButton(Context context){//创建button对象 & 为后来的悬浮按钮做准备
            super(context);
            this.context = context;
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            layoutParams = new WindowManager.LayoutParams();
            popuMenu = new PopupMenu(ModelEditorActivity.this , this);//按钮点击时的菜单
            for(String i :Utilities.添加界面的按键文字){
                popuMenu.getMenu().add(i);
            }
            popuMenu.setOnDismissListener(this);
            popuMenu.setOnMenuItemClickListener(this);
        }
        
        
        //处理按钮移动和按钮单击操作
        private int lastX;//你可以用float，但会出现按键漂移问题
        private int lastY;
        private float onDownX;
        private float onDownY;
        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            switch(event.getAction()){
                case event.ACTION_DOWN:
                    lastX = (int) (onDownX = event.getRawX());
                    lastY = (int) (onDownY = event.getRawY());
                    return true;
                case event.ACTION_MOVE:
                    layoutParams.x -= lastX-event.getRawX();
                    layoutParams.y -= lastY-event.getRawY();
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    windowManager.updateViewLayout(this , layoutParams);
                    return true;
                case event.ACTION_UP:
                    if((  Math.abs(onDownY-event.getRawY()) + Math.abs(onDownX-event.getRawX()) )< Utilities.偏移量){//判断是否点击按钮
                        popuMenu.show();
                        return true;
                    }
                    return false;
            }
            return false;
        }  
        
        
        
        //implements
        
        //运行此方法时添加按键
        @Override
        public void run()
        {
            layoutParams.type = layoutParams.FIRST_SUB_WINDOW+5;
            /*
             如果没有设置FLAG_NOT_FOCUSABLE，那么屏幕上弹窗之外的地方不能点击。
             如果设置了FLAG_NOT_FOCUSABLE，那么屏幕上弹窗之外的地方能够点击、但是弹窗上的EditText无法输入、键盘也不会弹出来。
             如果设置了FLAG_NOT_TOUCH_MODAL，那么屏幕上弹窗之外的地方能够点击、弹窗上的EditText也可以输入、键盘能够弹出来。
             FLAG_FULLSCREEN Activity窗口全屏，状态栏不显示。
            */
            layoutParams.flags = layoutParams.FLAG_NOT_TOUCH_MODAL|layoutParams.FLAG_NOT_FOCUSABLE|layoutParams.FLAG_FULLSCREEN;
            layoutParams.gravity=Gravity.TOP|Gravity.LEFT;
            layoutParams.format= android.graphics.PixelFormat.RGBA_8888;
            layoutParams.alpha= Utilities.按钮默认透明度;
            layoutParams.x = windowManager.getDefaultDisplay().getWidth()>>1;
            layoutParams.y = windowManager.getDefaultDisplay().getHeight()>>1;//除2
            layoutParams.width = (layoutParams.height = Utilities.getDefaultButtonSize(ModelEditorActivity.this));
            windowManager.addView(this , layoutParams);
        }  
        
        @Override
        public void onDismiss(PopupMenu p1)
        {
        }
        
        @Override
        public boolean onMenuItemClick(MenuItem p1)
        {
            if(p1.getTitle().toString()==Utilities.添加界面的按键文字[0]){//完成
                if(isOnAddEvent){
                    addFinished();//处理背景变化和 onTouchEvent
                }else{
                    ModelEditorActivity.this.finish();//此时应该保存模板
                    }
            }else if(p1.getTitle().toString()==Utilities.添加界面的按键文字[1]){//添加按钮
                addButtonStarted();
            }else{//添加触摸板
                
            }
            return false;
        }
    }
}
