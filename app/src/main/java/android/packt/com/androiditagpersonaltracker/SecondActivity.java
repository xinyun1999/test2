package android.packt.com.androiditagpersonaltracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class SecondActivity extends AppCompatActivity {
    private TextView textView;
    private double jing;
    private double wei;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textView = (TextView)findViewById(R.id.position_text_view);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_second);
        mapView = (MapView)findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();

        Intent intent = getIntent();
        jing = intent.getDoubleExtra("jing",0);
        wei = intent.getDoubleExtra("wei",0);
        textView.setText("经度："+jing+"\n"+"纬度"+wei+"\n");
        navigateTo(jing,wei);
        baiduMap.setMyLocationEnabled(true);

    }

    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }
    private void navigateTo(double jing,double wei){
        if (isFirstLocate) {
            Toast.makeText(this, "定位到所在区域",Toast.LENGTH_SHORT).show();
            LatLng ll = new LatLng(wei, jing);
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }
        MyLocationData.Builder locationBuilder = new MyLocationData.
                Builder();
        locationBuilder.latitude(wei);
        locationBuilder.longitude(jing);
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }
}
