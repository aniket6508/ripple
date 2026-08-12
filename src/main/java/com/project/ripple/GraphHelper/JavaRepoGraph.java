package com.project.ripple.GraphHelper;

import java.util.List;

record JavaRepoGraph(
        List<MethodNode> nodes,
        List<CallEdge> edges
){
}
