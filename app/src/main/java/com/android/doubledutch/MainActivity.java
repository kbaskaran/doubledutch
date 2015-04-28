package com.android.doubledutch;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "RottenTomatoes";
    private static final String RT_URL = "http://api.rottentomatoes.com/api/public/v1.0/lists/dvds/top_rentals.json?apikey=";
    private static final String API_KEY = "f8tmzfkbvdb5za7hzaw7dn98";

    private GetContentTask contentTask = null;
    private ListView moviesList;
    private CustomMovieAdapter customMovieAdapter;
    private ArrayList<MovieContent> topTenList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topTenList = new ArrayList<MovieContent>();
        contentTask = new GetContentTask();
        contentTask.execute(RT_URL + API_KEY);

        moviesList = (ListView) findViewById(R.id.list_movies);
        moviesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View item, int position, long rowId) {
                // Launch the detail view passing movie as an extra
                String text = ((TextView) item.findViewById(R.id.firstLine)).getText().toString();
                Toast.makeText(getApplicationContext(), "You clicked " + text, Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_profile) {
            //do something?
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contentTask != null) {
            contentTask.cancel(true);
        }
    }

    private class MovieContent {
        String title;
        int score;
        String thumbnailLink;
        String desc;

        public MovieContent(String t, int s, String b, String d) {
            title = t;
            score = s;
            thumbnailLink = b;
            desc = d;

        }

    }

    private void updateList(ArrayList<MovieContent> movieList) {
        moviesList.setAdapter(new CustomMovieAdapter(getApplicationContext(), movieList));
        //setListViewHeightBasedOnChildren(moviesList);
    }

    private class GetContentTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... url) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(url[0]));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    Log.d(TAG, "ok success");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString(); // deal with this string later
                } else {
                    Log.d(TAG, "request failed");
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (Exception e) {
                Log.d(TAG, "something went wrong while getting response" + e.toString());
            }
            return responseString;
        }

        // use the response from above to show the list
        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (response != null) {
                Log.d(TAG, "all good - inside of postexecute now");
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray movies = jsonResponse.getJSONArray("movies");

                    if (movies == null) {
                        Log.e(TAG, "may be got a bad response");
                        return;
                    }

                    int movieCount = movies.length(); // fixed to 10 in this case as api limits so

                    for (int i = 0; i < movieCount; i++) {
                        JSONObject movie = movies.getJSONObject(i);
                        MovieContent movieContent = new MovieContent(movie.getString("title"),
                                movie.getJSONObject("ratings").getInt("critics_score"),
                                movie.getJSONObject("posters").getString("thumbnail"), movie.getString("synopsis"));
                        topTenList.add(movieContent);
                    }

                    updateList(topTenList);
                } catch (JSONException e) {
                    Log.d(TAG, "parsing JSON response error!");
                }
            }
        }
    }

    public class CustomMovieAdapter extends ArrayAdapter<MovieContent> {
        public CustomMovieAdapter(Context context, ArrayList<MovieContent> movieList) {
            super(context, 0, movieList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            MovieContent movie = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.movie_list_item, null);
            }

            ImageView movieThumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            Picasso.with(getContext()).load(movie.thumbnailLink).into(movieThumbnail);
            TextView movieTitle = (TextView) convertView.findViewById(R.id.firstLine);
            movieTitle.setText(movie.title);
            TextView movieScore = (TextView) convertView.findViewById(R.id.secondLine);
            movieScore.setText("Rating: " + movie.score + "% " + movie.desc);

            return convertView;
        }

    }

    // how to dynamically change the listview
//    public static void setListViewHeightBasedOnChildren(ListView listView) {
//        ListAdapter listAdapter = listView.getAdapter();
//        if (listAdapter != null) {
//
//            int numberOfItems = listAdapter.getCount();
//
//            // Get total height of all items.
//            int totalItemsHeight = 0;
//            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
//                View item = listAdapter.getView(itemPos, null, listView);
//                item.measure(0, 0);
//                totalItemsHeight += item.getMeasuredHeight();
//            }
//
//            // Get total height of all item dividers.
//            int totalDividersHeight = listView.getDividerHeight() *
//                    (numberOfItems - 1);
//
//            // Set list height.
//            ViewGroup.LayoutParams params = listView.getLayoutParams();
//            params.height = totalItemsHeight + totalDividersHeight;
//            listView.setLayoutParams(params);
//            listView.requestLayout();
//        }
//    }

}