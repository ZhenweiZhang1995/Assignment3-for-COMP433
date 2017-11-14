package hanson.assignment3;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    EditText tagText;
    EditText sizeText;
    File photo;
    String path;
    SQLiteDatabase database;
    ImageView imgview;
    long fileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgview = (ImageView)findViewById(R.id.img);
        tagText = (EditText) findViewById(R.id.tag);
        sizeText = (EditText) findViewById(R.id.size);

        database =  openOrCreateDatabase("db", Context.MODE_PRIVATE, null);
        database.execSQL("DROP TABLE IF EXISTS Photos");
        database.execSQL("CREATE TABLE Photos (Path TEXT, Tag TEXT, Size INTEGER)");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        preview();

        File f = new File(path);
        fileSize = f.length() / 1024;
        sizeText.setText(""+fileSize);

    }

    public void capture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photo = null;
            try {
                photo = createImage();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photo != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
                preview();
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

            }
        }
    }

    public void save(View v){
        try {
            boolean exist = false;

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(path);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            Cursor c = database.rawQuery("SELECT * From Photos", null);
            c.moveToFirst();

            for (int i = 0; i < c.getCount(); i++) {
                for (int j = 0; j < c.getColumnCount(); j++) {
                    System.out.println(c.getString(j));
                    if (c.getString(j).equals(path)) {
                        exist = true;
                    }
                }
                c.moveToNext();
            }

            if (tagText.getText().length() == 0) {
                Toast toast = Toast.makeText(getApplicationContext(), "You need to define a tag", Toast.LENGTH_SHORT);
                toast.show();
            } else if (path.length() == 0) {
                Toast toast = Toast.makeText(getApplicationContext(), "You need to take a photo", Toast.LENGTH_SHORT);
                toast.show();
            } else if (!exist) {
                String[] arr = tagText.getText().toString().split(";");

                for (String e : arr) {
                    database.execSQL("INSERT INTO Photos VALUES ('" + path + "', '" + e
                            + "', '" + (int) fileSize + "')");
                }
                Toast toast = Toast.makeText(getApplicationContext(), "Photo saved to "  +path, Toast.LENGTH_SHORT);
                toast.show();
            }
        }catch(Exception ex){
            ex.printStackTrace();
            Toast toast = Toast.makeText(getApplicationContext(), "You haven't take a photo yet", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void load(View v){
        try {
            StringBuilder address;
            String query = null;

            if (tagText.getText().toString().length() > 0 && sizeText.getText().toString().length() == 0) {
                String[] arr = tagText.getText().toString().split(";");
                for (String e : arr) {
                    query = "Select Photos.Path FROM Photos WHERE Photos.Tag = '" + e + "'";
                }
            } else if (tagText.getText().toString().length() == 0 && sizeText.getText().toString().length() > 0) {
                int size = Integer.parseInt(sizeText.getText().toString());
                int minSize = (int) (size * 0.75);
                int maxSize = (int) (size * 1.25);
                query = "Select Photos.Path FROM Photos WHERE Photos.Size > '" + Integer.toString(minSize)
                        + "' AND  Photos.Size < '" + Integer.toString(maxSize) + "'";
            } else if (tagText.getText().toString().length() > 0 && sizeText.getText().toString().length() > 0) {
                String[] arr = tagText.getText().toString().split(";");
                int size = Integer.parseInt(sizeText.getText().toString());
                int minSize = (int) (size * 0.75);
                int maxSize = (int) (size * 1.25);
                for (String e : arr) {
                    query = "Select Photos.Path FROM Photos WHERE Photos.Size > '" + Integer.toString(minSize)
                            + "' AND  Photos.Size < '" + Integer.toString(maxSize) + "' AND Photos.Tag = '"
                            + e + "'";
                }
            }

            Cursor c = database.rawQuery(query, null);
            c.moveToFirst();
            address = new StringBuilder();

            for (int i = 0; i < c.getCount(); i++) {
                for (int j = 0; j < c.getColumnCount(); j++) {
                    address.append(c.getString(j));
                    break;
                }
                c.moveToNext();
            }

            if (address.length() > 0) {
                path = address.toString();
                File f = new File(path);
                fileSize = f.length() / 1024;
                sizeText.setText("" + fileSize);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Cannot find photo", Toast.LENGTH_SHORT);
                toast.show();
            }

            path = address.toString();
            preview();
        }catch(Exception ex){
            ex.printStackTrace();
            Toast toast = Toast.makeText(getApplicationContext(), "You haven't take a photo yet", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private File createImage() throws IOException {
        File storage = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                "assignment3",".jpg",storage
        );
        path = image.getAbsolutePath();

        return image;
    }

    private void preview() {
        try {
            imgview.setVisibility(View.VISIBLE);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            final Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            imgview.setImageBitmap(bitmap);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


}
