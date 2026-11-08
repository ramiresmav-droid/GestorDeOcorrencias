package pt.pendentes.app;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
  DB db; LinearLayout root, list; Spinner filter; EditText search, locationSearch;
  String selectedPhoto=""; ImageView photoPreview;
  final String[] priorities={"Melhoria","Pendente","Urgente","Muito urgente"};
  int[] colors={0xff2e7d32,0xff546e7a,0xffef6c00,0xffc62828};
  int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
  TextView text(String s,int size){ TextView t=new TextView(this); t.setText(s);t.setTextSize(size);t.setTextColor(0xff263238);t.setPadding(dp(4),dp(6),dp(4),dp(6));return t; }
  @Override public void onCreate(Bundle b){super.onCreate(b);db=new DB(this);show();}
  void show(){
    root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(0xfff5f7fa);setContentView(root);
    TextView title=text("Pendentes",28);title.setTypeface(null,1);root.addView(title);
    Button add=new Button(this);add.setText("+ Nova situação");add.setOnClickListener(v->edit(-1));root.addView(add);
    search=new EditText(this);search.setSingleLine(true);search.setHint("Pesquisar por título");root.addView(search);
     locationSearch=new EditText(this);locationSearch.setSingleLine(true);locationSearch.setHint("Filtrar por localização / morada");root.addView(locationSearch);
    filter=new Spinner(this);String[] opts={"Todas as prioridades","Muito urgente","Urgente","Pendente","Melhoria","Concluídas"};filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));root.addView(filter);
    ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh();} public void afterTextChanged(android.text.Editable e){}});
    locationSearch.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){} public void onTextChanged(CharSequence s,int a,int b,int c){refresh();} public void afterTextChanged(android.text.Editable e){}});
     filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> a){}public void onItemSelected(android.widget.AdapterView<?> a,View v,int p,long id){refresh();}});
    refresh();
  }
   void refresh(){
     if(list==null)return;
     list.removeAllViews();
     String term=search.getText().toString().trim();
     String address=locationSearch.getText().toString().trim();
     int sel=filter.getSelectedItemPosition();
     Cursor c=db.getReadableDatabase().rawQuery(
       "SELECT id,title,location,priority,done FROM tasks WHERE title LIKE ? AND location LIKE ? ORDER BY done ASC,priority DESC,id DESC",
       new String[]{"%"+term+"%","%"+address+"%"});
     int count=0;
     while(c.moveToNext()){
       int id=c.getInt(0),p=c.getInt(3),done=c.getInt(4);
       String name=c.getString(1),loc=c.getString(2);
       if(sel==5 && done==0)continue;
       if(sel>0&&sel<5&&(done==1||p!=4-sel))continue;
       if(sel!=5&&done==1)continue;
       count++;
       LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
       row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(10),dp(10));
       row.setBackgroundColor(Color.WHITE);
       LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,0,0,dp(5));list.addView(row,rp);
       LinearLayout info=new LinearLayout(this);info.setOrientation(1);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
       TextView h=text(name,16);h.setTypeface(null,1);h.setSingleLine(true);h.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(h);
       if(loc!=null&&!loc.isEmpty()){TextView addressLine=text("📍 "+loc,12);addressLine.setSingleLine(true);addressLine.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(addressLine);}
       TextView tag=text(priorities[p]+(done==1?" · Concluída":""),12);tag.setTextColor(colors[p]);row.addView(tag);
       TextView arrow=text("  ›",23);row.addView(arrow);
       row.setOnClickListener(v->details(id));
     }
     c.close();if(count==0)list.addView(text("Sem situações nesta lista.",16));
   }

   void details(int id){
     Cursor c=db.getReadableDatabase().rawQuery("SELECT title,description,location,priority,done,photo FROM tasks WHERE id=?",new String[]{""+id});
     if(!c.moveToFirst()){c.close();refresh();return;}
     String name=c.getString(0),desc=c.getString(1),loc=c.getString(2),photo=c.getString(5);
     int p=c.getInt(3),done=c.getInt(4);c.close();
     LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(16),dp(8),dp(16),dp(8));
     TextView status=text(priorities[p]+(done==1?" · Concluída":""),16);status.setTextColor(colors[p]);content.addView(status);
     if(desc!=null&&!desc.isEmpty()){content.addView(text("Descrição",14));content.addView(text(desc,16));}
     if(loc!=null&&!loc.isEmpty()){content.addView(text("Localização / morada",14));content.addView(text(loc,16));
       Button map=new Button(this);map.setText("Ver no mapa");content.addView(map);
       map.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q="+Uri.encode(loc)));try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Não existe uma app de mapas instalada",Toast.LENGTH_LONG).show();}});
     }
     if(photo!=null&&!photo.isEmpty()){ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setMaxHeight(dp(350));try{iv.setImageBitmap(BitmapFactory.decodeFile(photo));content.addView(iv);}catch(Exception ignored){}}
     ScrollView scroll=new ScrollView(this);scroll.addView(content);
     AlertDialog dialog=new AlertDialog.Builder(this).setTitle(name).setView(scroll)
       .setNegativeButton("Fechar",null).setNeutralButton("Editar",null)
       .setPositiveButton(done==1?"Reabrir":"Concluir",null).create();
     dialog.setOnShowListener(v->{
       dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(w->{dialog.dismiss();edit(id);});
       dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{ContentValues cv=new ContentValues();cv.put("done",done==1?0:1);db.getWritableDatabase().update("tasks",cv,"id=?",new String[]{""+id});dialog.dismiss();refresh();});
     });
     dialog.show();
   }

  EditText field(LinearLayout l,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);l.addView(e);return e;}
  void edit(int id){String name="",desc="",loc="";int priority=1;selectedPhoto="";
    if(id!=-1){Cursor c=db.getReadableDatabase().rawQuery("SELECT title,description,location,priority,photo FROM tasks WHERE id=?",new String[]{""+id});if(c.moveToFirst()){name=c.getString(0);desc=c.getString(1);loc=c.getString(2);priority=c.getInt(3);selectedPhoto=c.getString(4);}c.close();}
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(20),dp(6),dp(20),dp(6));
    EditText title=field(form,"Título da situação *",name),description=field(form,"Descrição",desc),location=field(form,"Localização / morada",loc);description.setMinLines(2);
    Button choosePhoto=new Button(this);choosePhoto.setText("Adicionar / alterar fotografia");form.addView(choosePhoto);
    photoPreview=new ImageView(this);photoPreview.setAdjustViewBounds(true);photoPreview.setMaxHeight(dp(160));form.addView(photoPreview);updatePhotoPreview();
    choosePhoto.setOnClickListener(v->{Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT);picker.setType("image/*");picker.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(picker,101);});
    Button removePhoto=new Button(this);removePhoto.setText("Remover fotografia");form.addView(removePhoto);removePhoto.setOnClickListener(v->{selectedPhoto="";updatePhotoPreview();});
    form.addView(text("Prioridade",15));Spinner pr=new Spinner(this);pr.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,priorities));pr.setSelection(priority);form.addView(pr);
    AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(id==-1?"Nova situação":"Editar situação").setView(form).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null);
    if(id!=-1)builder.setNeutralButton("Eliminar",(d,w)->new AlertDialog.Builder(this).setMessage("Eliminar esta situação?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(a,b)->{db.getWritableDatabase().delete("tasks","id=?",new String[]{""+id});refresh();}).show());
    AlertDialog dialog=builder.create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{String t=title.getText().toString().trim();if(t.isEmpty()){title.setError("Indique um título");return;}ContentValues cv=new ContentValues();cv.put("title",t);cv.put("description",description.getText().toString().trim());cv.put("location",location.getText().toString().trim());cv.put("priority",pr.getSelectedItemPosition());cv.put("photo",selectedPhoto);if(id==-1)db.getWritableDatabase().insert("tasks",null,cv);else db.getWritableDatabase().update("tasks",cv,"id=?",new String[]{""+id});dialog.dismiss();refresh();}));dialog.show();
  }
  void updatePhotoPreview(){if(photoPreview==null)return;photoPreview.setImageDrawable(null);if(selectedPhoto!=null&&!selectedPhoto.isEmpty())photoPreview.setImageBitmap(BitmapFactory.decodeFile(selectedPhoto));}
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=101||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
    try(InputStream in=getContentResolver().openInputStream(data.getData())){File dest=new File(getFilesDir(),"photo_"+System.currentTimeMillis()+".jpg");try(OutputStream out=new FileOutputStream(dest)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}selectedPhoto=dest.getAbsolutePath();updatePhotoPreview();}
    catch(Exception e){Toast.makeText(this,"Não foi possível carregar a fotografia",Toast.LENGTH_LONG).show();}
  }
  static class DB extends SQLiteOpenHelper {DB(Context c){super(c,"pendentes.db",null,2);}public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',location TEXT NOT NULL DEFAULT '',priority INTEGER NOT NULL DEFAULT 1,done INTEGER NOT NULL DEFAULT 0,photo TEXT NOT NULL DEFAULT '')");}public void onUpgrade(SQLiteDatabase d,int old,int now){if(old<2)d.execSQL("ALTER TABLE tasks ADD COLUMN photo TEXT NOT NULL DEFAULT ''");} }
}
