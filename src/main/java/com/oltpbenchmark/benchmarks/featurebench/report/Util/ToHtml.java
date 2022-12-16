package com.oltpbenchmark.benchmarks.featurebench.report.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.hubspot.jinjava.Jinjava;

public class ToHtml {

    public static Jinjava jinjava = new Jinjava();

    public static void main(String args[]) throws IOException {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser
                    .parse(new FileReader("src/main/java/com/oltpbenchmark/benchmarks/featurebench/report/data.json"));
            JSONArray clusteArray = (JSONArray) obj.get("cluster");
            File input = new File("src/main/java/com/oltpbenchmark/benchmarks/featurebench/report/index.html");
            Document doc = Jsoup.parse(input, "UTF-8");

            // Overview
            Element div = doc.select("#overview").first();
            generateOverview(div, clusteArray);

            // Summary Vertical Chart
            Element script = doc.getElementById("javascript");
            script.text("");
            generateChartVertical(script, "briefSummaryVertical", (JSONArray) obj.get("workloads"));

            // Summary Chart
            generateChartHorizontal(script, "briefSummaryHorizontal", (JSONArray) obj.get("workloads"));

            JSONArray workloads = (JSONArray) obj.get("workloads");
            div = doc.getElementById("workload");
            for (int i = 0; i < workloads.size(); i++)
                generateWorkload(div, (JSONObject) workloads.get(i));

            // Save Html
            saveHtml(doc, input);

        } catch (ParseException e) {

            e.printStackTrace();
        }

    }

    public static void generateOverview(Element div, JSONArray clusteArray) {

        div.text("");
        for (int i = 0; i < clusteArray.size(); i++) {
            String overviewhtml = jinjava.render(
                    "<div class='py-2'><div class='text-primary text-lg font-semibold leading-3 cursor-pointer'>{{ name }}</div><ul class='mt-2 marker:text-accent list-outside list-disc px-5'><li><div class='flex flex-row items-center text-primary'><div class='w-24'>DB-Type :</div><div class=''>{{ DB_type }}</div></div></li><li><div class='flex flex-row items-center text-primary'><div class='w-24'>Url :</div><div class=''>{{ Url }}</div></div></li><li><div class='flex flex-row items-center text-primary'><div class='w-24'>Version :</div><div class=''>{{ Version }}</div></div></li></ul></div>",
                    (Map) clusteArray.get(i));
            div.append(overviewhtml);
        }
    }

    public static void generateWorkload(Element div, JSONObject workload) {
        String text = "";

        text = text
                + String.format("<div class='text-2xl font-semibold text-blue-900'>%s</div>", workload.get("workload"));
        text = text +
                "<div class='bg-section w-full mt-4 p-5 shadow-lg'><div class='text-primary text-lg font-semibold leading-3 cursor-pointer'>Queries</div><div class='grid grid-cols-1 divide-y divide-accent'>";
        JSONArray queries = (JSONArray) workload.get("queries");
        for (int i = 0; i < queries.size(); i++) {
            String query = jinjava.render(
                    "<div class='py-2'><ul class='mt-2 marker:text-accent list-outside list-disc px-5'><li><div class='flex flex-row items-center text-primary'><div class='w-24'>Name :</div><div class=''>{{ name }}</div></div></li><li><div class='flex flex-row items-center text-primary'><div class='w-24'>Weight :</div><div class=''>{{weight}}</div></div></li><li><div class='flex flex-row items-center text-primary'><div class='w-24'>Query :</div><div class='font-semibold italic tracking-wide'>{{query}}</div></div></li></ul></div>",
                    (Map) queries.get(i));
            text = text + query;
        }
        text = text + "</div>";
        div.append(text);

        div.append(generateAvergaeLatency(workload));

    }

    public static String generateAvergaeLatency(JSONObject workload) {
        String text = "<div class='accordion mt-10'><div class='accordion-box'><div class='text-xl font-semibold text-blue-900 mt-4 label cursor-pointer bg-section px-2 py-2 rounded-md hover:border-b-2 hover:border-accent transition duration-150 shadow'>Avergae Latency</div><div class='flex flex-row mt-6 justify-between content'>";

        // Client Side Latency Div
        text = text
                + "<div class='bg-section p-2 w-[20%] shadow-lg rounded-md px-8 flex flex-col'><div class='text-primary font-light w-full mt-4'>Client Side Latency (ms)</div><div class='flex flex-col mt-2 grow justify-around'>";

        Map<String, Object> average_latency = (Map) workload.get("average_latency");

        int i = 0;
        for (Map.Entry<String, Object> entry : average_latency.entrySet()) {
            Map<String, Object> context = new HashMap<>();
            context.put("key", entry.getKey());
            context.put("value", entry.getValue());
            String renderedTemplate = jinjava.render(
                    "<div class='flex flex-row w-full'><div class='text-primary font-semibold flex-grow'>{{ key }}</div><div class='text-accent font-semibold w-[40%]'>{{ value }}</div></div>",
                    context);
            if (i + 1 < average_latency.size()) {
                renderedTemplate = renderedTemplate + "<div class='h-[1px] bg-accent w-full'></div>";
            }
            text = text + renderedTemplate;
            i++;
        }

        text = text + "</div></div>";

        return text;

    }

    public static void generateChartVertical(Element script, String elementId, JSONArray workloads) {
        script.append(String.format("const %s = document.getElementById('%s');\n", elementId, elementId));
        Map<String, Object> context = new HashMap<>();
        String title = "";
        String labels = "";
        String data = "";

        for (Object workload : workloads) {
            JSONObject workloadObject = (JSONObject) workload;
            if (title == "") {
                title = (String) workloadObject.get("workload");
            } else {
                title = title + " vs " + workloadObject.get("workload");
            }
            JSONObject map = (JSONObject) workloadObject.get("average_latency");

            for (Object key : map.keySet()) {
                if (labels == "") {
                    labels = "'" + (String) key + "'";
                    data = ((Number) map.get(key)).toString();
                } else {
                    labels = labels + "," + "'" + key + "'";
                    data = data + "," + ((Number) map.get(key)).toString();
                }
            }

        }
        title = "'" + title + "'";

        context.put("labels",
                labels);
        context.put("data", data);
        context.put("title", title);
        context.put("id", elementId);
        String renderedTemplate = jinjava.render(
                "new Chart( {{ id }}, { type: 'bar', data: { labels: [ {{ labels }} ], datasets: [{ label: '# Client Side Latency (ms)', data: [{{data}}], backgroundColor: [ '#60a5fa', '#60a5fa', '#60a5fa', '#818cf8', '#818cf8', '#818cf8', ],borderRadius: 10, }, ], },options:{ maintainAspectRatio: false, scales: { y: { beginAtZero: true, }, }, plugins: { title: { display: true, text: {{title }}, }, }, },}); \n",
                context);

        script.append(renderedTemplate);

    }

    public static void generateChartHorizontal(Element script, String elementId, JSONArray workloads) {
        script.append(String.format("const %s = document.getElementById('%s'); \n", elementId, elementId));
        Map<String, Object> context = new HashMap<>();
        String title = "";
        String labels = "";
        String data = "";

        for (Object workload : workloads) {
            JSONObject workloadObject = (JSONObject) workload;
            if (title == "") {
                title = (String) workloadObject.get("workload");
            } else {
                title = title + " vs " + workloadObject.get("workload");
            }
            JSONObject map = (JSONObject) workloadObject.get("average_latency");

            for (Object key : map.keySet()) {
                if (labels == "") {
                    labels = "'" + (String) key + "'";
                    data = ((Number) map.get(key)).toString();
                } else {
                    labels = labels + "," + "'" + key + "'";
                    data = data + "," + ((Number) map.get(key)).toString();
                }
            }

        }
        title = "'" + title + "'";

        context.put("labels",
                labels);
        context.put("data", data);
        context.put("title", title);
        context.put("id", elementId);
        String renderedTemplate = jinjava.render(
                "new Chart( {{ id }}, { type: 'bar', data: { labels: [ {{ labels }} ], datasets: [{ label: '# Client Side Latency (ms)', data: [{{data}}], backgroundColor: [ '#60a5fa', '#60a5fa', '#60a5fa', '#818cf8', '#818cf8', '#818cf8', ],borderRadius: 10, barThickness: 20 }, ], },options:{  indexAxis: 'y', maintainAspectRatio: false, scales: { y: { beginAtZero: true, }, }, plugins: { title: { display: true, text: {{title }}, }, }, },}); \n",
                context);

        script.append(renderedTemplate);

    }

    public static void saveHtml(Document doc, File input) {

        PrintWriter writer;
        try {
            writer = new PrintWriter(input, "UTF-8");
            writer.write(doc.html());
            writer.flush();
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {

            e.printStackTrace();
        }

    }
};