package org.iotacontrolcenter.rest.resource;

import org.iotacontrolcenter.api.*;
import org.iotacontrolcenter.dto.*;
import org.iotacontrolcenter.iota.agent.ActionFactory;
import org.iotacontrolcenter.iota.agent.Agent;
import org.iotacontrolcenter.iota.agent.action.util.AgentUtil;
import org.iotacontrolcenter.properties.locale.Localizer;
import org.iotacontrolcenter.properties.source.PropertySource;

import javax.servlet.http.HttpServletRequest;
import javax.swing.*;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;

public class IccrServiceImpl implements IccrService {

    private Agent agent = Agent.getInstance();
    private Localizer localizer = Localizer.getInstance();
    private PropertySource propSource = PropertySource.getInstance();


    public IccrServiceImpl() {
        //System.out.println("creating new IccrServiceImpl");
    }

    @Override
    public Response getConfigProperties(HttpServletRequest request) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r = Response.status(HttpURLConnection.HTTP_OK);

        IccrPropertyListDto propList = new IccrPropertyListDto();

        for(String key : propSource.getPropertyKeys()) {
            propList.addProperty(new IccrPropertyDto(key, propSource.getString(key)));
        }
        //propList.addProperty(propSource.getIotaNeighbors());
        r.entity(propList);
        return r.build();
    }

    @Override
    public Response getConfigProperty(HttpServletRequest request, String key) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;

        if(key == null || key.isEmpty()) {
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, localizer.getLocalText("invalidRequestNoKey")));
            return r.build();
        }

        r = Response.status(HttpURLConnection.HTTP_OK);
        IccrPropertyDto prop;

        if(key.equals(PropertySource.IOTA_NEIGHBORS_PROP)) {
            prop = propSource.getIotaNeighbors();
        }
        else {
            prop = new IccrPropertyDto(key, propSource.getString(key));
        }

        r.entity(prop);
        return r.build();
    }

    @Override
    public Response getIotaNbrsConfig(HttpServletRequest request) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r = Response.status(HttpURLConnection.HTTP_OK);
        IccrIotaNeighborsPropertyDto prop = propSource.getIotaNeighbors();

        r.entity(prop);
        return r.build();
    }

    @Override
    public Response updateConfigProperties(HttpServletRequest request, IccrPropertyListDto properties) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;

        if(properties == null) {
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, localizer.getLocalText("invalidUpdateNoProperties")));
            return r.build();
        }

        System.out.println("updateConfigProperties");

        try {
            for(IccrPropertyDto prop : properties.getProperties()) {
                System.out.println(prop.getKey() + " -> " + prop.getValue());
                propSource.setProperty(prop.getKey(), prop.getValue());
            }
            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(new SimpleResponse(true, localizer.getLocalText("updateSuccess")));
        }
        catch(Exception e) {
            System.out.println("updateConfigProperties exception: ");
            e.printStackTrace();
            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        return r.build();
    }

    @Override
    public Response updateConfigProperty(HttpServletRequest request, String key, IccrPropertyDto prop) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;

        if(prop == null) {
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, localizer.getLocalText("invalidUpdateNoProperties")));
            return r.build();
        }

        System.out.println("updateConfigProperty " + key);

        try {
            if(key.equals(PropertySource.IOTA_NEIGHBORS_PROP)) {
                propSource.setIotaNeighbors((IccrIotaNeighborsPropertyDto)prop);
            }
            else {
                propSource.setProperty(prop.getKey(), prop.getValue());
            }
            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(new SimpleResponse(true, localizer.getLocalText("updateSuccess")));
        }
        catch(Exception e) {
            System.out.println("updateConfigProperty exception: ");
            e.printStackTrace();
            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        return r.build();
    }

    @Override
    public Response updateIotaNbrsConfig(HttpServletRequest request, IccrIotaNeighborsPropertyDto prop) {
        if(!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;

        if(prop == null) {
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, localizer.getLocalText("invalidUpdateNoProperties")));
            return r.build();
        }

        System.out.println("updateIotaNbrsConfig ");

        // If IOTA was active, need to update its nbrs list, we'll just remove all prev, then update config,
        // then add all current
        boolean wasActive = AgentUtil.isIotaActive();

        if(wasActive) {

            IccrIotaNeighborsPropertyDto prevNbrs = propSource.getIotaNeighbors();
            if(prevNbrs != null && !prevNbrs.getNbrs().isEmpty()) {

                System.out.println("updateIotaNbrsConfig: IOTA was active, removing neighbors");

                try {
                    ActionResponse resp1 = agent.action(ActionFactory.REMOVENEIGHBORS, null);
                } catch (Exception e) {
                    System.out.println("updateIotaNbrsConfig remove nbrs exception: ");
                    e.printStackTrace();
                }
            }
            else {
                System.out.println("updateIotaNbrsConfig: IOTA was active, but no neighbors were configured");
            }
        }

        boolean ok = true;
        try {
            propSource.setIotaNeighbors(prop);

            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(new SimpleResponse(true, localizer.getLocalText("updateSuccess")));
        }
        catch(Exception e) {
            ok = false;
            System.out.println("updateIotaNbrsConfig exception: ");
            e.printStackTrace();
            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        if(ok && wasActive) {
            System.out.println("updateIotaNbrsConfig: IOTA was active, adding current neighbors");

            try {
                ActionResponse resp2 = agent.action(ActionFactory.ADDNEIGHBORS, null);
            }
            catch(Exception e) {
                System.out.println("updateIotaNbrsConfig add nbrs exception: ");
                e.printStackTrace();
            }
        }

        return r.build();
    }

    @Override
    public Response doIotaAction(HttpServletRequest request, String action, IccrPropertyListDto actionProps) {
        if (!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;

        try {
            ActionResponse resp = agent.action(action, actionProps);
            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(resp);
        }
        catch(IllegalArgumentException iae) {
            System.out.println("doIotaAction illegal arg error: " + iae.getMessage());
            iae.printStackTrace();
            // Message is already localized
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, iae.getMessage()));
        }
        catch(IllegalStateException ise) {
            System.out.println("doIotaAction illegal state error: " + ise.getMessage());
            ise.printStackTrace();
            // Message is already localized

            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, ise.getMessage()));
        }
        catch(Exception e) {
            System.out.println("doIotaAction server error: " + e.getMessage());
            e.printStackTrace();
            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        return r.build();
    }

    @Override
    public Response getIotaNodeInfo(HttpServletRequest request) {
        if (!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;
        try {
            ActionResponse resp = agent.action(ActionFactory.NODEINFO, null);
            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(resp);
        }
        catch(IllegalArgumentException iae) {
            System.out.println("getIotaNodeInfo exception: ");
            iae.printStackTrace();

            // Message is already localized
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, iae.getMessage()));
        }
        catch(IllegalStateException ise) {
            System.out.println("getIotaNodeInfo exception: ");
            ise.printStackTrace();

            // Message is already localized
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, ise.getMessage()));
        }
        catch(Exception e) {
            System.out.println("getIotaNodeInfo exception: ");
            e.printStackTrace();

            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        return r.build();
    }

    @Override
    public Response getIotaNeighbors(HttpServletRequest request) {
        if (!authorizedRequest(request)) {
            return unauthorizedResponse(request);
        }
        Response.ResponseBuilder r;
        try {
            ActionResponse resp = agent.action(ActionFactory.NEIGHBORS, null);
            r = Response.status(HttpURLConnection.HTTP_OK);
            r.entity(resp);
        }
        catch(IllegalArgumentException iae) {
            System.out.println("getIotaNeighbors exception: ");
            iae.printStackTrace();

            // Message is already localized
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, iae.getMessage()));
        }
        catch(IllegalStateException ise) {
            System.out.println("getIotaNeighbors exception: ");
            ise.printStackTrace();

            // Message is already localized
            r = Response.status(HttpURLConnection.HTTP_BAD_REQUEST).
                    entity(new SimpleResponse(false, ise.getMessage()));
        }
        catch(Exception e) {
            System.out.println("getIotaNeighbors exception: ");
            e.printStackTrace();

            r = Response.status(HttpURLConnection.HTTP_INTERNAL_ERROR).
                    entity(new SimpleResponse(false, localizer.getLocalText("serverError") + ": " + e.getLocalizedMessage()));
        }

        return r.build();
    }

    private boolean authorizedRequest(HttpServletRequest request) {
        String apiAccessKey = request.getHeader(ResourceUtil.API_ACCESS_KEY_PROP);
        return apiAccessKey != null && !apiAccessKey.isEmpty() && apiAccessKey.equals(propSource.getApiKey());
    }

    private Response unauthorizedResponse(HttpServletRequest request) {
        return Response.status(HttpURLConnection.HTTP_UNAUTHORIZED).
                entity(new SimpleResponse(false, "Not authorized to use " + request.getRequestURL())).
                build();
    }
}