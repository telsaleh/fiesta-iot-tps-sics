package uk.ac.surrey.ee.iot.smartics.endpoint.restlet.resource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import uk.ac.surrey.ee.iot.smartics.annotator.FiestaAnnotator;
import uk.ac.surrey.ee.iot.smartics.model.ics.Observations;

public class TpsHandler extends ServerResource {

    public String sIcsURL = "http://131.227.88.96:5000/getLastObservations";

//    @Get("txt")
//    public String toString() {
//        return "Account of user \"" + "TEST" + "\"";
//    }
    @Post("json")
    public Representation handlePost(Representation entity) throws IOException {

        String tpsRequest = entity.getText();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String result = getSmartIcsObservation(tpsRequest);
        StringRepresentation response = new StringRepresentation(result);
        response.setMediaType(MediaType.APPLICATION_JSON);
        
        return response;
    }

    public String getSmartIcsObservation(String tpsRequest) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        final Context context = new Context();
        Client client = new Client(new Context(), Protocol.HTTP);
        client.getContext().getParameters().add("maxConnectionsPerHost", "5");
        client.getContext().getParameters().add("maxTotalConnections", "5");
        final ClientResource smartIcsClientResource = new ClientResource(context, sIcsURL);
        smartIcsClientResource.setNext(client);
        smartIcsClientResource.accept(MediaType.APPLICATION_JSON);
        System.out.println("TPS request: " + tpsRequest);
        String errorMessage="";
        try {
            Representation result = smartIcsClientResource
                    .post(new StringRepresentation(tpsRequest, MediaType.APPLICATION_JSON));
            smartIcsClientResource.release();
            try {
                Observations obs = objectMapper.readValue(result.getStream(), Observations.class);
                FiestaAnnotator fa = new FiestaAnnotator();
                String annotatedOb = fa.annotateObservations(obs);               
//                String annotatedOb = "OK";
                return annotatedOb;
            } catch (IOException ioex) {
                Logger.getLogger(TpsHandler.class.getName()).log(Level.SEVERE, null, ioex);
                System.out.println("IO Error....ERROR IS THIS: " + ioex.getLocalizedMessage()); 
            }
        } catch (ResourceException ex) {
            Logger.getLogger(TpsHandler.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("ERROR IS THIS: " + ex.getLocalizedMessage()); 
            errorMessage = ex.getLocalizedMessage();
        }
        return errorMessage;
        
    }

}