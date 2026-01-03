package edu.hm.ccwi.matilda.gateway.service;

import edu.hm.ccwi.matilda.gateway.exception.GatewayException;

import java.util.List;

public interface StateService {

    /**
     *
     * @return
     */
    List stateProjectProfiles() throws GatewayException;

}
