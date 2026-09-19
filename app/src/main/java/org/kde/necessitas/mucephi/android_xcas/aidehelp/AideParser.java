package org.kde.necessitas.mucephi.android_xcas.aidehelp;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leonel on 24/11/17.
 */

public final class AideParser {

    private static List<JSONObject> mDataset = null;
    private static AideParser instance = null;

    public static List<JSONObject> getDataset(Context context, String index_lang_help){

        if(instance == null){
            instance = new AideParser();
        }

        return instance.getJSONhelpDataset(context, index_lang_help);
    }

    public static void reset(){
        mDataset = null;
    }

    public static List<JSONObject> getJSONhelpDataset(Context context, String index_lang_help){

        if(mDataset == null){
            mDataset = new ArrayList<JSONObject>();
        }else{
            return mDataset;
        }

        try {

            InputStream instream = context.getAssets().open("help_xcas.json");

            if (instream != null) {
                parseLines(new BufferedReader(new InputStreamReader(instream)), index_lang_help);
                instream.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return mDataset;
    }

    /** Fallback language used when the requested help language has no text.
     *  Covers the untranslated slots in help_xcas.json (5, 6, 7) and any
     *  future language id without full coverage. */
    static final String FALLBACK_LANG = "2";

    /** Parses one JSON help object per line; extracted for unit testing without Android assets. */
    static void parseLines(BufferedReader buffreader, String indexLangHelp) throws Exception {
        try {
            String line;

            while ((line = buffreader.readLine()) != null){
                JSONObject function = new JSONObject(line);
                JSONObject langs = function.getJSONObject("langs");
                String describe = describeFor(langs, indexLangHelp);
                function.put("describe", describe);
                function.put("related", JArrayToList(function.getJSONArray("related")));
                function.put("examples", JArrayToList(function.getJSONArray("examples")));
                mDataset.add(function);
            }
        } finally {
            buffreader.close();
        }
    }

    /** Returns the help text for the requested language, falling back to
     *  English when the entry is missing or blank (whitespace-only).
     *  Unknown language ids without any usable fallback still throw
     *  JSONException, preserving the previous strict behaviour. */
    static String describeFor(JSONObject langs, String indexLangHelp) throws JSONException {
        String describe = langs.optString(indexLangHelp, null);
        if (describe != null && !describe.trim().isEmpty()) {
            return describe;
        }
        String fallback = langs.optString(FALLBACK_LANG, null);
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }
        return langs.getString(indexLangHelp);
    }

    private static List<String> JArrayToList(JSONArray array){


        int len = array.length();
        List<String> list = new ArrayList<String>(len);

        for (int i=0; i<len; i++){
            try {
                list.add(array.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return list;
    }
}
