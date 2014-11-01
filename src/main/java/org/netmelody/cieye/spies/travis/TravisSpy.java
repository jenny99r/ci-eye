package org.netmelody.cieye.spies.travis;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.domain.Status;
import org.netmelody.cieye.core.domain.TargetDetail;
import org.netmelody.cieye.core.domain.TargetDigest;
import org.netmelody.cieye.core.domain.TargetDigestGroup;
import org.netmelody.cieye.core.domain.TargetId;
import org.netmelody.cieye.core.observation.CiSpy;
import org.netmelody.cieye.core.observation.Contact;
import org.netmelody.cieye.core.observation.KnownOffendersDirectory;
import org.netmelody.cieye.spies.travis.jsondomain.Repo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.netmelody.cieye.core.domain.Status.UNKNOWN;

public class TravisSpy implements CiSpy {

    private final TravisCommunicator communicator;
    private final Map<TargetId, Repo> recognisedRepos;

    public TravisSpy(String endpoint, KnownOffendersDirectory directory, Contact contact) {
        communicator = new TravisCommunicator(endpoint, contact);
        recognisedRepos = new HashMap<TargetId, Repo>();
    }

    @Override
    public TargetDigestGroup targetsConstituting(Feature feature) {
        Repo repo = communicator.getRepo(feature);

        final List<TargetDigest> digests = newArrayList();

        if (repo != null) {
            final TargetDigest targetDigest = new TargetDigest(repo.id, communicator.getEndpoint() + "/repos/" + repo.slug, repo.slug, UNKNOWN);
            digests.add(targetDigest);
            recognisedRepos.put(targetDigest.id(), repo);
        }
        return new TargetDigestGroup(digests);
    }

    @Override
    public TargetDetail statusOf(TargetId target) {
        Repo repo = recognisedRepos.get(target);
        if (null == repo) {
            return null;
        }
        //return buildTypeAnalyser.targetFrom(buildType);
        return new TargetDetail("Yo", "www.goo.co.uk", "Yo me", Status.GREEN, 67676766767L);
    }

    @Override
    public boolean takeNoteOf(TargetId target, String note) {
        return false;
    }

}
