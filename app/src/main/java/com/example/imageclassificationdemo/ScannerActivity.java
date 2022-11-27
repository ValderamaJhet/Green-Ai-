package com.example.imageclassificationdemo;

import static android.app.Activity.RESULT_OK;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class            ScannerActivity extends Fragment {

    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private  int imageSizeX;
    private  int imageSizeY;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;

    private Bitmap bitmap;
    private List<String> labels;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    ImageView imageView;
    Uri imageuri;
    Button buclassify,btngallery,btncamera;
    TextView classitext,textClassType,textClassPercentage,textRemarks,textDisease;
    LinearLayout layoutScanInfo;
    String a,b,c,d;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_scanner);
        df.format(1234123);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.activity_scanner, null);
        imageView=(ImageView)root.findViewById(R.id.image);
        buclassify=(Button)root.findViewById(R.id.classify);
        buclassify.setEnabled(false);
        btngallery=(Button)root.findViewById(R.id.btn_gallery);
        btncamera=(Button)root.findViewById(R.id.btncamera);
        textClassType = root.findViewById(R.id.textClassType);
        textClassPercentage = root.findViewById(R.id.textClassPercentage);
        textRemarks = root.findViewById(R.id.textRemarks);
        textDisease = root.findViewById(R.id.textDisease);
        classitext=(TextView)root.findViewById(R.id.classifytext);
        layoutScanInfo = root.findViewById(R.id.layoutScanInfo);
        btngallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),12);
            }
        });

        btncamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, 0);

                }
        });

        buclassify.setOnClickListener(v -> {
            a="";b="";c="";d="";
            float broccoli= getClassification("broccoli.tflite","broccoli.txt");
            float bokchoy= getClassification("bokchoy.tflite","bokchoy.txt");
            float cabbage= getClassification("cabbage.tflite","cabbage.txt");
            float lettuce= getClassification("lettuce.tflite","lettuce.txt");

            if((bokchoy >=broccoli) && (bokchoy>=cabbage) && (bokchoy>=lettuce))
            {
                String str = a;
                String[] parts = str.split("_");
                classitext.setText("Bok Choy (Brassica chinensis)");
                displayScanInfo(provideResult(bokchoy*100,parts[2]));
            }
            else if((broccoli>=bokchoy) && (broccoli>=cabbage) && (broccoli>=lettuce))
            {

                String str = b;
                String[] parts = str.split("_");
                classitext.setText("Broccoli (Brassica Oleracea var. italica)");
                displayScanInfo(provideResult(broccoli*100,parts[2]));
                //classitext.setText(result+"\nConfidence level: "+ df.format(broccoli*100)+"%");
            }
            else  if((cabbage >=broccoli) && (cabbage >= bokchoy) && (cabbage>=lettuce))
            {
                String str = c;
                String[] parts = str.split("_");
                classitext.setText("Cabbage(Brassica oleracea var. capitata)");
                displayScanInfo(provideResult(broccoli*100,parts[2]));


            }
            else  if((lettuce >= broccoli) && (lettuce>=cabbage) && (lettuce>=bokchoy))
            {
                String str = d;
                String[] parts = str.split("_");
                classitext.setText("Lettuce (Lactuca sativa)");
                displayScanInfo(provideResult(broccoli*100,parts[2]));
            }
            if(broccoli<0.6 || cabbage<0.6 || bokchoy<0.6 || lettuce<0.6 ){
                classitext.setText("Unknown image, check image quality and make sure it is one of featured vegetables...");
                layoutScanInfo.setVisibility(View.GONE);
            }

        });

        return root;

    }

    public ScannerActivity() {
        // Required empty public constructor
    }


    private float getClassification(String modelfile,String modellabel)
    {

        try{
            tflite=new Interpreter(loadmodelfile(requireActivity(),modelfile));
        }catch (Exception e) {
            e.printStackTrace();
        }

        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        inputImageBuffer = loadImage(bitmap);

        tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());

        return showresult(modellabel);
    }
    
    
    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("model.tflite");
       // AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("newmodel.tflite");

        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private MappedByteBuffer loadmodelfile(Activity activity,String modelfile) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd(modelfile);
        // AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("newmodel.tflite");

        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }


    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocesDequantizeOp(){
        return new DequantizeOp(0, (float) (1/255.0));
    }

    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private float showresult(String label){


        try{
            labels = FileUtil.loadLabels(requireActivity(),label);
          //  labels = FileUtil.loadLabels(this,"newdict.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
                        
        float maxValueInMap =(Collections.max(labeledProbability.values()));
        Log.d("Probability",maxValueInMap * 100 +"");

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                //classitext.setText(entry.getKey().toLowerCase()+" "+maxValueInMap);
                //classitext.setText(entry.getKey().toLowerCase());
                if(label.equals("bokchoy.txt"))
                {
                    a=entry.getKey().toLowerCase();
                }
                else if(label.equals("broccoli.txt"))
                {
                    b=entry.getKey().toLowerCase();
                }
                else if(label.equals("cabbage.txt"))
                {
                    c=entry.getKey().toLowerCase();
                }
                else if(label.equals("lettuce.txt"))
                {
                    d=entry.getKey().toLowerCase();
                }
            }
        }
        return maxValueInMap;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==12 && resultCode==RESULT_OK && data!=null) {
            imageuri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageuri);
                imageView.setImageBitmap(bitmap);
                buclassify.setEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode==0 && resultCode==RESULT_OK && data!=null) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
            buclassify.setEnabled(true);
        }
    }
    private void displayScanInfo(ScanResult scanResult) {
        layoutScanInfo.setVisibility(View.VISIBLE);
        if (scanResult.classPercent <= 60) {
            textClassPercentage.setTextColor(Color.parseColor("#D32F2F"));
        }
        textClassType.setText(scanResult.getClassType());
        textClassPercentage.setText(scanResult.getClassPercent()+"%");
        textRemarks.setText(scanResult.getRemarks());
        textDisease.setText(scanResult.getDisease());

    }
    private ScanResult provideResult(float confidenceLevel,String disease) {
        ScanResult scanResult = new ScanResult();
        if (confidenceLevel <= 60) {
            scanResult = new ScanResult("C",confidenceLevel,"Rejected",disease);
        } else if (confidenceLevel > 60 && confidenceLevel < 80) {

            scanResult = new ScanResult("B",confidenceLevel,"Marketable in local place only",disease);
        } else if(confidenceLevel >= 80) {
            scanResult = new ScanResult("C",confidenceLevel,"Marketable","");
        }
        return scanResult;
    }
}

