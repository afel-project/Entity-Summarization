package utils_package;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.List;
import java.util.Map;

public class WebUtils {
    public static HttpClient client = new HttpClient();

    /*
     * Does the actual call of the Web Service, using a HttpClient which executes the GetMethod.
     */
    public static String request(String url) {
        Client client1 = Client.create();
        System.out.println(url);

        WebResource wbres = client1.resource(url);
        ClientResponse cr = wbres.accept("application/json").get(ClientResponse.class);
        int status = cr.getStatus();
        String response = cr.getEntity(String.class);

        if (status == 200)
            return response;

        return "";
    }

    public static String post(String url, List<Map.Entry<String, String>> urlParameters) {
        try {
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(url);
            // add header
            for (Map.Entry<String, String> name_val : urlParameters) {
                method.addParameter(name_val.getKey(), name_val.getValue());
            }

            //Set the results type, which will be JSON.
            method.addRequestHeader(new Header("Accept", "application/json"));
            method.addRequestHeader(new Header("content-type", "application/x-www-form-urlencoded"));

            int response = client.executeMethod(method);
            if (response != HttpStatus.SC_OK) {
                System.out.println("Method failed: " + method.getStatusText());
            }
            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            String response_str = new String(responseBody);
            method.releaseConnection();
            method = null;
            client = null;

            return response_str;
        } catch (Exception e) {
        }
        return "";
    }
}