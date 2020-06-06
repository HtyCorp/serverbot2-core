package io.mamish.serverbot2.workflow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.mamish.serverbot2.framework.exception.server.RequestHandlingException;
import io.mamish.serverbot2.sharedconfig.CommonConfig;
import io.mamish.serverbot2.sharedutil.IDUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UbuntuAmiLocator {

    // Observed from browser/GUI: query string param seems to be epoch millis, to prevent caching.
    private static final String baseReleasesUri = "https://cloud-images.ubuntu.com/locator/ec2/releasesTable?_=";
    private static final HttpClient http = HttpClient.newBuilder().build();

    private static final Map<Function<UbuntuAmiInfo,String>,String> AMI_CRITERIA = Map.of(
            UbuntuAmiInfo::getRegion, getRegion(),
            UbuntuAmiInfo::getArch, "amd64",
            UbuntuAmiInfo::getName, "bionic",
            UbuntuAmiInfo::getVersion, "18.04 LTS",
            UbuntuAmiInfo::getType, "hvm:ebs-ssd"
    );

    public UbuntuAmiInfo getIdealAmi() {
        String rawData = fetchReleaseData();
        List<UbuntuAmiInfo> infoList = parseReleaseData(rawData);
        return getAmiByCriteria(infoList);
    }

    private UbuntuAmiInfo getAmiByCriteria(List<UbuntuAmiInfo> options) {
        List<UbuntuAmiInfo> finalOptions = options.stream().filter(ami ->
                AMI_CRITERIA.entrySet().stream().allMatch(e -> {
                        var actualValue = e.getKey().apply(ami);
                        var matchValue = e.getValue();
                        return actualValue.equals(matchValue);
        })).collect(Collectors.toList());

        if (finalOptions.size() == 0) {
            throw new RequestHandlingException("No AMI options found matching criteria");
        }
        if (finalOptions.size() > 1) {
            throw new RequestHandlingException("Found multiple AMI options matching criteria");
        }

        return finalOptions.get(0);
    }

    private List<UbuntuAmiInfo> parseReleaseData(String data) {
        JsonArray releaseArray = JsonParser.parseString(data).getAsJsonObject().get("aaData").getAsJsonArray();

        return arrayToStream(releaseArray).filter(JsonElement::isJsonArray).map(row -> {
            JsonArray nestedArray = row.getAsJsonArray();
            String[] fields = arrayToStream(nestedArray).map(JsonElement::getAsString).toArray(String[]::new);
            return new UbuntuAmiInfo(fields);
        }).collect(Collectors.toList());
    }

    private Stream<JsonElement> arrayToStream(JsonArray array) {
        return IntStream.range(0, array.size()).mapToObj(array::get);
    }

    private String fetchReleaseData() {
        try {
            return http.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(baseReleasesUri + IDUtils.epochMillis()))
                    .build(),
                    HttpResponse.BodyHandlers.ofString()
            ).body();
        } catch (IOException | InterruptedException e) {
            throw new RequestHandlingException("Unable to fetch Ubuntu releases data", e);
        }
    }

    private static String getRegion() {

        if (CommonConfig.ENABLE_MOCK.notNull()) {
            return "ap-southeast-2";
        }

        String region = System.getenv("AWS_REGION");
        if (region == null) {
            throw new IllegalStateException("Region environment variable missing: ensure this runs on Lambda");
        }

        return region;

    }

}
