package com.example.a1605563.netcoresample;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final String STRING_URL = "http://pnstage.netcore.co.in/netcore-interview-textfile.txt";
    private final String STRING_YANDEX_URL = "https://dictionary.yandex.net/api/v1/dicservice.json/lookup?";
    private final String API_KEY = "dict.1.1.20190425T184201Z.1f19e6cda6029a53.e2c5a3d19e91483fe3a69a16431cd18d875022c4";
    private TextView textView;
    TextView tv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv2);
        StringRequest request = new StringRequest(StringRequest.Method.GET, STRING_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                       // Log.wtf("MainActivity", "Completed execution"+response.substring(0,10000));
                        AnalyzeTask task = new AnalyzeTask();
                        task.execute(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private class AnalyzeTask extends AsyncTask<String, Void, HashMap<String, Integer>>{

        @Override
        protected HashMap<String, Integer> doInBackground(String... strings) {

            HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
            String response = strings[0];

            response = response.replaceAll("[^a-zA-Z0-9\\s]", "");

            String arr[] = response.split("\\s+");

            Log.wtf("TAG", arr.length+"");

            for(int i=0; i<arr.length; i++){
                int count = 0;
                String word = arr[i];
                for(int j=1+1;j<arr.length;j++){
                    if(arr[j].equals(word))
                        count++;
                }

                if(!hashMap.containsKey(word))
                    hashMap.put(word, count);
            }

            return hashMap;

        }

        @Override
        protected void onPostExecute(HashMap<String, Integer> stringIntegerHashMap) {
            super.onPostExecute(stringIntegerHashMap);

            allWordsOccurences(stringIntegerHashMap);

            topTenWords(stringIntegerHashMap);


        }

        private void allWordsOccurences(HashMap<String, Integer> stringIntegerHashMap){
            Log.wtf("TAG", stringIntegerHashMap.toString());
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for(String key : stringIntegerHashMap.keySet()){

                JSONObject object = new JSONObject();
                try {
                    object.put(key, stringIntegerHashMap.get(key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(object);
            }

            try {
                jsonObject.put("response", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            textView.setText(jsonObject.toString());
        }

        private void topTenWords(HashMap<String, Integer> hashMap){

            List<Map.Entry<String, Integer> > list =
                    new LinkedList<Map.Entry<String, Integer> >(hashMap.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
                public int compare(Map.Entry<String, Integer> o1,
                                   Map.Entry<String, Integer> o2)
                {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
            for (Map.Entry<String, Integer> aa : list) {
                temp.put(aa.getKey(), aa.getValue());
            }

            Set<String> keys = temp.keySet();
            Iterator iterator = keys.iterator();

            JSONArray array = new JSONArray();

            for(int i=0;i<10;i++){

                String word = iterator.next().toString();
                int count = temp.get(word);
                fetch(word, count, array);

            }


        }

        private void fetch(final String word, final int count, final JSONArray array){

            String url = buildUrl(word);

            StringRequest request = new StringRequest(StringRequest.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            String pos = getPos(response);
                            JSONArray arr = getArr(response);

                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("word", word);

                                JSONObject outputObject = new JSONObject();
                                outputObject.put("count", count);
                                outputObject.put("pos", pos);
                                outputObject.put("syn", arr);

                                jsonObject.put("output", outputObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            array.put(jsonObject);

                            if(array.length() == 10){
                                Log.wtf("TAG", array.toString());
                                //TODO: put this result in another textview.
                                tv2.setText(array.toString());
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            queue.add(request);
        }

        private String getPos(String response){

            String pos = "";

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("def");
                JSONObject object = (JSONObject)jsonArray.get(0);
                pos = object.getString("pos");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return  pos;
        }

        private JSONArray getArr(String response){

            JSONArray array = new JSONArray();

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("def");
                JSONObject object = (JSONObject)jsonArray.get(0);
                array = object.getJSONArray("tr");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return array;
        }

        private String buildUrl(String word){

            Uri baseUrl = Uri.parse(STRING_YANDEX_URL);
            Uri.Builder builder = baseUrl.buildUpon();
            builder.appendQueryParameter("key", API_KEY);
            builder.appendQueryParameter("lang", "en-en");
            builder.appendQueryParameter("text", word);

            return builder.toString();

        }
    }
}