package org.netmelody.cieye.spies.travis;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.observation.Contact;
import org.netmelody.cieye.spies.travis.jsondomain.Repo;

public class TravisCommunicator {
    private final String endpoint;
    private final Contact contact;

    public TravisCommunicator(String endpoint, Contact contact) {
        this.endpoint = endpoint;
        this.contact = contact;
    }

    public Repo getRepo(Feature feature) {
        return contact.makeJsonRestCall(endpoint + "/repos/" + feature.name(), Repo.class);
    }

    public String getEndpoint() {
        return endpoint;
    }
}
