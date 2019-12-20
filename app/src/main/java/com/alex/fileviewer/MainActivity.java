package com.alex.fileviewer;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 영상추가를 위한 커스텀 explore
 * zip파일만 보여주며, zip파일 내 이미지 추출하여 표시.
 */
public class MainActivity extends Activity {

    private TextView zipview_title;//선택된 디바이스의 닉네임, 디렉토리명.
    private ImageView zipview_back_btn; //뒤로가기.
    private GridView zipview_grid; //gridview
    private Button btn_add_zipfile; //추가 버튼
    private LinearLayout zipview_progress_layout; //프로그레스바
    private String m_root = "";//파일 경로.



    private ArrayList<Itemss> ziplist;
    private GridAdapter gridAdapter;


    //액티비티 전달
    private ArrayList<String> zippathList;
    private ArrayList<String> pngpathList;
    private ArrayList<String> zipsizeList;

    private int checkedDevice=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        zipview_title=findViewById(R.id.zipview_title);
        zipview_back_btn=findViewById(R.id.zipview_back_btn);
        zipview_grid=findViewById(R.id.zipview_grid);
        btn_add_zipfile=findViewById(R.id.btn_add_zipfile);
        zipview_progress_layout=findViewById(R.id.zipview_progress_layout);



        zipview_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        //닉네임, 경로용.
//        String device_nickname = App.SDinfo.nick_name;
//        Log.d("check nickname", " "+device_nickname);
        //닉네임 설정.
//        zipview_title.setText(device_nickname);
        //경로설정.
//        m_root=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/"+device_nickname+"/";
        Log.d("check folder path", m_root);


        new ListTask().execute(m_root);

        //영상추가 버튼
        //TODO: 체크된 영상 목록 확인하여, String array로 관련 정보 전달.
        // 전달 정보 : 1. zip파일 경로, 2. png파일 경로, 3.파일 용량
        btn_add_zipfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zipview_progress_layout.setVisibility(View.VISIBLE);
                checkedDevice=0;
                zippathList=new ArrayList<>();
                pngpathList=new ArrayList<>();
                zipsizeList=new ArrayList<>();


                //1. 체크된 영상 값 전달
                for(int i=0; i<ziplist.size(); i++){
                    if(ziplist.get(i).getSelected()){//체크된 항목일 경우 등록
                        zippathList.add(ziplist.get(i).path);
                        pngpathList.add(ziplist.get(i).pngPath);
                        zipsizeList.add(ziplist.get(i).size);
                        checkedDevice++;
                    }
                }

                Intent backintent = new Intent();
                //array로 전달?

//                backintent.putarr
                if(checkedDevice>0){
                    backintent.putStringArrayListExtra("zippath", zippathList);
                    backintent.putStringArrayListExtra("pngpath", pngpathList);
                    backintent.putStringArrayListExtra("zipsize", zipsizeList);
                    setResult(RESULT_OK, backintent);
                    zipview_progress_layout.setVisibility(View.GONE);
                    finish();
                }else{
                    zipview_progress_layout.setVisibility(View.GONE);
                    Log.d("no check", "device");
                    Toast.makeText(MainActivity.this, "선택하세요", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }


    public class Itemss implements Comparable<Itemss>{
        private String name;
        private String size;
        private String path;
        //        private Bitmap image;
        private String pngPath;
        private boolean selected;
        public Itemss(String n,String s,String p, String imgPath, Boolean selec)
        {
            name = n;
            size = s;
            path = p;
            pngPath=imgPath;
            selected=selec;

        }


        public String getName()
        {
            return name;
        }
        public String getSize()
        {
            return size;
        }
        public String getPath()
        {
            return path;
        }
        public String getImage() {
            return pngPath;
        }
        public boolean getSelected(){return selected;}

        public void setSelected(boolean sele){this.selected=sele;}
        @Override
        public int compareTo(Itemss itemss) {
            if(this.name != null)
                return this.name.toLowerCase().compareTo(itemss.getName().toLowerCase());
            else
                throw new IllegalArgumentException();
        }
    }

    private class ListTask extends AsyncTask<String, Void, String> {
        File curDir;
        File[] dirs;

        ArrayList<Itemss> fls;
        String sResult;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            zipview_progress_layout.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... strings) {
            curDir=new File(strings[0]);

            dirs=curDir.listFiles();
            ziplist=new ArrayList<>();
            try{
                if(dirs==null) {//TODO: 폴더 없음 분기점 처리
                    Log.w("dir", "length : 0, 영상 없음.");

                }
                for(File ff : dirs){
                    if(ff.isDirectory()){
                        File[] fbuf = ff.listFiles();
                        int buf = 0;
                        if(fbuf!=null){
                            buf=fbuf.length;
                        }else{
                            buf=0;
                        }
//                    String num_item = String.valueOf(buf);

                    }else{
                        if(!ff.getName().endsWith("png")) {
                            Bitmap bit = getBitmapFromzip(m_root + ff.getName());
                            //TODO: 비트맵 로컬 저장.

                            String pngPath = saveBitmap(bit, ff.getName());
                            int siz = (int)ff.length();
                            String size="";

                            if(siz>1000000){
                                size=""+siz/1000000+"MB";
                            }else if(siz>1000){
                                size=""+siz/1000+"KB";
                            }else{
                                size=""+siz+"byte";
                            }



                            ziplist.add(new Itemss(ff.getName(), size, ff.getAbsolutePath(), pngPath, false));
                        }
                    }
                }
                sResult="success";
            }

            catch (Exception e){
                e.printStackTrace();
                sResult="fail";
            }
            Collections.sort(ziplist);
            gridAdapter=new GridAdapter(ziplist);

            return sResult;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d("check s", ""+s);
            if(s.equals("success")){
                Log.d("success", "YES");
            }else{
                Log.d("success", "failed");

            }
            zipview_grid.setAdapter(gridAdapter);
            zipview_progress_layout.setVisibility(View.GONE);

        }
    }




    private class GridAdapter extends BaseAdapter {
        ImageView img1;
        TextView filename;
        TextView filesize;
        ArrayList<Itemss> items;
        CheckBox check;
        int id;


        public GridAdapter(ArrayList<Itemss> objects){
            this.items=objects;
        }



        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Itemss getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if(view==null){
                LayoutInflater inflater=(LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view=inflater.inflate(R.layout.grid_zipviewer,null);

            }
            img1 = view.findViewById(R.id.zipview_grid_img1);
            filename = view.findViewById(R.id.zipview_grid_filename);
            filesize=view.findViewById(R.id.zipview_grid_filesize);
            check=view.findViewById(R.id.zipview_grid_check);
            Itemss o = items.get(i);

            String pngpath = o.getImage();
            Log.d("check pngpath", pngpath);
            File png = new File(pngpath);

//            img1.setImageBitmap(b);
            Glide.with(MainActivity.this).load(png).into(img1);
            filename.setText(o.getName());


            filesize.setText(o.getSize());

            if(o.getSelected()){//선택되었다면,
                check.setChecked(true);
            }else{
                check.setChecked(false);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isSelected = items.get(i).getSelected();
                    items.get(i).setSelected(!isSelected);
                    ziplist=items;

                    notifyDataSetChanged();
                }
            });
            check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isSelected = items.get(i).getSelected();
                    items.get(i).setSelected(!isSelected);
                    ziplist=items;
                    notifyDataSetChanged();
                }
            });

            return view;
        }
    }


    public Bitmap getBitmapFromzip(final String zipfilePath){
        Log.d("get bitmap", "from "+ zipfilePath);
        Bitmap result=null;
        try{
            FileInputStream fis = new FileInputStream(zipfilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = null;
            while((ze=zis.getNextEntry())!=null){
                if(ze.getName().contains(".png")){
                    result= BitmapFactory.decodeStream(zis);
                    break;
                }
            }
            fis.close();
            zis.close();
        }catch (FileNotFoundException fe){
            Log.d("get bitmap status", " Error opening zip file");
            fe.printStackTrace();
        } catch (IOException e) {
            Log.d("get bitmap status", " Error opening zip file");
            e.printStackTrace();
        }

        return result;
    }

    private String saveBitmap(Bitmap b, String zipname){
        String pngpath="";
        Bitmap bitmap=b;
        FileOutputStream out = null;
        try {
            String bitmapname =zipname.replace(".zip", ".png");
            File tempfile = new File(m_root, bitmapname);

            out = new FileOutputStream(tempfile);

            b.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();

            pngpath=tempfile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pngpath;
    }
}
