package org.netmelody.cieye.spies.travis;

import org.netmelody.cieye.core.domain.CiServerType;
import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.observation.CiSpy;
import org.netmelody.cieye.core.observation.CodeBook;
import org.netmelody.cieye.core.observation.CommunicationNetwork;
import org.netmelody.cieye.core.observation.KnownOffendersDirectory;
import org.netmelody.cieye.core.observation.ObservationAgency;

import java.text.SimpleDateFormat;

public class TravisObservationAgency implements ObservationAgency{

    @Override
    public boolean canProvideSpyFor(CiServerType type) {
        return "TRAVIS".equals(type.name());
    }

    @Override
    public CiSpy provideSpyFor(Feature feature, CommunicationNetwork network, KnownOffendersDirectory directory) {
        final CodeBook codeBook = new CodeBook(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
            .withCredentials(feature.username(), feature.password());
        return new TravisSpy(feature.endpoint(), directory, network.makeContact(codeBook));
    }
}
