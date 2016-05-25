package entitySummarization_api;



import edu.l3s.algorithm.EntitySummarization;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.jws.WebMethod;
import javax.jws.WebService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Created by ranyu on 5/24/16.
 */


@WebService(serviceName = "ES")
@Path("/ES")
@Stateless()
public class restService {
    @GET
    @WebMethod
    @Path("/entitySum")
    @Produces(MediaType.TEXT_HTML)

    public String entitySum(@QueryParam("type") final String type,
                             @QueryParam("query") final String query,
                             @QueryParam("disambiguation") final String dis_page
    ) throws IOException, ParseException, InterruptedException {
        EntitySummarization es = new EntitySummarization();
        es.load_index(type);
        String res =  es.summarize(query, type, dis_page);

        return res;
    }
}