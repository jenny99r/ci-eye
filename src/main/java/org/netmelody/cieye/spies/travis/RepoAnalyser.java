package org.netmelody.cieye.spies.travis;

import org.netmelody.cieye.core.domain.Status;
import org.netmelody.cieye.core.domain.TargetDetail;
import org.netmelody.cieye.spies.travis.jsondomain.Repo;

public class RepoAnalyser {
    private TravisCommunicator communicator;

    public RepoAnalyser(TravisCommunicator communicator) {
        this.communicator = communicator;
    }

    public TargetDetail targetFrom(Repo originalRepo) {
        Repo repo = communicator.getRepo(originalRepo);

        Status status;
        switch (repo.last_build_status) {
            case 0: status = Status.GREEN; break;
            case 1: status = Status.BROKEN; break;
            default: status = Status.UNKNOWN; break;
        }
        return new TargetDetail(
            originalRepo.id,
            communicator.getEndpoint() + "/repos/" + originalRepo.slug,
            originalRepo.slug,
            status,
            repo.last_build_started_at.getTime());
    }
}
