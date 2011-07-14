package org.netmelody.cieye.spies.teamcity;

import static org.netmelody.cieye.core.domain.Percentage.percentageOf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netmelody.cieye.core.domain.RunningBuild;
import org.netmelody.cieye.core.domain.Sponsor;
import org.netmelody.cieye.core.domain.Status;
import org.netmelody.cieye.core.domain.Target;
import org.netmelody.cieye.core.observation.KnownOffendersDirectory;
import org.netmelody.cieye.spies.teamcity.jsondomain.Build;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildType;
import org.netmelody.cieye.spies.teamcity.jsondomain.BuildTypeDetail;
import org.netmelody.cieye.spies.teamcity.jsondomain.Change;
import org.netmelody.cieye.spies.teamcity.jsondomain.ChangeDetail;

public final class BuildTypeAnalyser {

    private final TeamCityRestRequester requester;
    private final String endpoint;
    private final KnownOffendersDirectory detective;

    public BuildTypeAnalyser(TeamCityRestRequester requester, String endpoint, KnownOffendersDirectory detective) {
        this.requester = requester;
        this.endpoint = endpoint;
        this.detective = detective;
    }
    
    public Target targetFrom(BuildType buildType) {
        final BuildTypeDetail buildTypeDetail = requester.detailsFor(buildType);
        
        if (buildTypeDetail.paused) {
            return new Target(endpoint + buildType.href, buildType.webUrl, buildType.name, Status.DISABLED);
        }
        
        final Set<Sponsor> sponsors = new HashSet<Sponsor>();
        final List<RunningBuild> runningBuilds = new ArrayList<RunningBuild>();
        long startTime = 0L;
            
        for(Build build : requester.runningBuildsFor(buildType)) {
            final BuildDetail buildDetail = requester.detailsOf(build);
            startTime = Math.max(buildDetail.startDateTime(), startTime);
            sponsors.addAll(sponsorsOf(buildDetail));
            runningBuilds.add(new RunningBuild(percentageOf(build.percentageComplete), buildDetail.status()));
        }
        
        Status currentStatus = Status.GREEN;
        final Build lastCompletedBuild = requester.lastCompletedBuildFor(buildTypeDetail);
        if (null != lastCompletedBuild) {
            currentStatus = lastCompletedBuild.status();
            if (runningBuilds.isEmpty() || Status.BROKEN.equals(currentStatus)) {
                final BuildDetail buildDetail = requester.detailsOf(lastCompletedBuild);
                startTime = Math.max(buildDetail.startDateTime(), startTime);
                sponsors.addAll(sponsorsOf(buildDetail));
                currentStatus = buildDetail.status();
            }
        }
        
        return new Target(endpoint + buildType.href, buildType.webUrl, buildType.name, currentStatus, startTime, runningBuilds, sponsors);
    }

    private Set<Sponsor> sponsorsOf(BuildDetail build) {
        return detective.search(analyseChanges(build));
    }

    private String analyseChanges(BuildDetail build) {
        if (build.changes == null || build.changes.count == 0) {
            return "";
        }
        
        final List<Change> changes = requester.changesOf(build);
        
        final StringBuilder result = new StringBuilder();
        for (Change change : changes) {
            final ChangeDetail changeDetail = requester.detailedChangesOf(change);
            result.append(changeDetail.username);
            result.append(' ');
            result.append(changeDetail.comment);
            result.append(' ');
        }
        
        return result.toString();
    }
}
