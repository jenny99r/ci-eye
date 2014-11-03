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
        return getRepo(feature.name());
    }

    public Repo getRepo(Repo repo) {
        return getRepo(repo.slug);
    }

    private Repo getRepo(String slug) {
        return contact.makeJsonRestCall(endpoint + "/repos/" + slug, Repo.class);
    }

    public String getEndpoint() {
        return endpoint;
    }
}
