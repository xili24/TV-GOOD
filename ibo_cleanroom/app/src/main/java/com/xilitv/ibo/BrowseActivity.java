package com.xilitv.ibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BrowseActivity extends Activity {
    private String type;
    private XtreamClient api;
    private TextView header;
    private ListView list;
    private ProgressBar progress;
    private final ArrayList<Item> items = new ArrayList<>();
    private boolean showingCategories = true;

    static final class Item {
        String id;
        String title;
        String ext = "";
        boolean category;
        boolean series;

        Item(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    interface Loader {
        ArrayList<Item> run() throws Exception;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        type = getIntent().getStringExtra("type");
        if (type == null) type = "live";
        api = new XtreamClient(AppPrefs.portal(this), AppPrefs.user(this), AppPrefs.pass(this));

        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 32), Ui.dp(this, 24), Ui.dp(this, 32), Ui.dp(this, 24));

        header = Ui.title(this, labelForType(), 28);

        progress = new ProgressBar(this);

        list = new ListView(this);
        list.setDividerHeight(1);
        list.setBackgroundColor(Ui.BG);
        list.setCacheColorHint(Ui.BG);
        list.setFocusable(true);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 60)));
        root.addView(progress, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        list.setOnItemClickListener((parent, view, position, id) -> select(items.get(position)));
        loadCategories();
    }

    private String labelForType() {
        if ("vod".equals(type)) return "MOVIES";
        if ("series".equals(type)) return "SERIES";
        return "LIVE TV";
    }

    private void loadCategories() {
        showingCategories = true;
        header.setText(labelForType() + "  •  GROUPS");

        final String action =
                "vod".equals(type) ? "get_vod_categories"
                        : "series".equals(type) ? "get_series_categories"
                        : "get_live_categories";

        load(() -> {
            JSONArray a = api.array(action, null, null);
            ArrayList<Item> out = new ArrayList<>();

            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;

                Item item = new Item(
                        o.optString("category_id"),
                        o.optString("category_name", "Group")
                );
                item.category = true;
                out.add(item);
            }
            return out;
        });
    }

    private void loadCategory(String categoryId, String name) {
        showingCategories = false;
        header.setText(labelForType() + "  •  " + name);

        final String action =
                "vod".equals(type) ? "get_vod_streams"
                        : "series".equals(type) ? "get_series"
                        : "get_live_streams";

        load(() -> {
            JSONArray a = api.array(action, "category_id", categoryId);
            ArrayList<Item> out = new ArrayList<>();

            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;

                String itemId = "series".equals(type)
                        ? o.optString("series_id")
                        : o.optString("stream_id");

                String title = o.optString("name", o.optString("title", "Item"));
                Item item = new Item(itemId, title);
                item.ext = o.optString("container_extension", "");
                item.series = "series".equals(type);
                out.add(item);
            }
            return out;
        });
    }

    private void loadEpisodes(String seriesId, String seriesName) {
        showingCategories = false;
        header.setText("SERIES  •  " + seriesName);

        load(() -> {
            JSONObject info = api.object("get_series_info", "series_id", seriesId);
            ArrayList<Item> out = new ArrayList<>();

            Object episodes = info.opt("episodes");

            if (episodes instanceof JSONObject) {
                JSONObject seasons = (JSONObject) episodes;
                Iterator<String> keys = seasons.keys();

                while (keys.hasNext()) {
                    String season = keys.next();
                    appendEpisodes(seasons.optJSONArray(season), out, season);
                }
            } else if (episodes instanceof JSONArray) {
                appendEpisodes((JSONArray) episodes, out, "");
            }

            return out;
        });
    }

    private static void appendEpisodes(JSONArray arr, ArrayList<Item> out, String season) {
        if (arr == null) return;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;

            String epNo = o.optString("episode_num", String.valueOf(i + 1));
            String title = o.optString("title", "Episode " + epNo);

            if (season != null && !season.isEmpty()) {
                title = "S" + season + " • E" + epNo + "  " + title;
            }

            Item item = new Item(o.optString("id"), title);
            item.ext = o.optString("container_extension", "mp4");
            out.add(item);
        }
    }

    private void select(Item item) {
        if (item.category) {
            loadCategory(item.id, item.title);
            return;
        }

        if (item.series) {
            loadEpisodes(item.id, item.title);
            return;
        }

        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("title", item.title);

        if ("live".equals(type)) {
            i.putExtra("url", api.liveUrl(item.id, item.ext.isEmpty() ? "ts" : item.ext));
            i.putExtra("fallback", api.liveUrl(item.id, "m3u8"));
        } else if ("vod".equals(type)) {
            i.putExtra("url", api.movieUrl(item.id, item.ext));
        } else {
            i.putExtra("url", api.seriesUrl(item.id, item.ext));
        }

        startActivity(i);
    }

    private void load(Loader loader) {
        progress.setVisibility(View.VISIBLE);
        list.setVisibility(View.INVISIBLE);

        new Thread(() -> {
            try {
                ArrayList<Item> out = loader.run();
                runOnUiThread(() -> show(out));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    list.setVisibility(View.VISIBLE);
                    Toast.makeText(
                            this,
                            e.getMessage() == null ? "Load failed" : e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void show(List<Item> data) {
        items.clear();
        items.addAll(data);

        ArrayList<String> labels = new ArrayList<>();
        for (Item i : items) labels.add(i.category ? "▸  " + i.title : i.title);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView v = (TextView) super.getView(position, convertView, parent);
                        v.setTextColor(Ui.WHITE);
                        v.setTextSize(20);
                        v.setGravity(Gravity.CENTER_VERTICAL);
                        v.setPadding(
                                Ui.dp(BrowseActivity.this, 18),
                                0,
                                Ui.dp(BrowseActivity.this, 18),
                                0
                        );
                        v.setMinHeight(Ui.dp(BrowseActivity.this, 58));
                        v.setBackgroundColor(Ui.PANEL);
                        return v;
                    }
                };

        list.setAdapter(adapter);
        progress.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);

        if (!items.isEmpty()) list.setSelection(0);
        list.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (!showingCategories) {
            loadCategories();
        } else {
            super.onBackPressed();
        }
    }
}
