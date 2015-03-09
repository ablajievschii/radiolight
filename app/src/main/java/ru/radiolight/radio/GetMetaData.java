package ru.radiolight.radio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

public final class GetMetaData {

    static String getMeta(String url) throws IOException{
        Log.i(GetMetaData.class.getName(), url);
        String bodyText;
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        Log.i(GetMetaData.class.getName(), "SDK: " + currentApiVersion);
        if (currentApiVersion < android.os.Build.VERSION_CODES.KITKAT){
            IcyStreamMeta icy = new IcyStreamMeta(new URL(url));
            bodyText = icy.getArtistTitle();
        } else {
            String page = loadPage(url+"/7");
            Document doc = Jsoup.parse(page);
            Element link = doc.select("body").first();
            bodyText = link.text();
        }

        return bodyText;
    }

    static String loadPage(String link) throws IOException{
        URL url = new URL(link);
        URLConnection spoof = url.openConnection();

        //Spoof the connection so we look like a web browser
        spoof.setRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)" );
        BufferedReader in = new BufferedReader(new InputStreamReader(spoof.getInputStream()));
        String strLine;
        String finalHTML = "";
        //Loop through every line in the source
        while ((strLine = in.readLine()) != null){
            finalHTML += strLine;
        }
        String[] data = finalHTML.split(",",7);
        finalHTML = data[data.length-1];

        return finalHTML;
    }
}
