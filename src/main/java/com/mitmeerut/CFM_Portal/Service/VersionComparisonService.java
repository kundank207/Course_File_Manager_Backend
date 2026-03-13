package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.CourseFile;
import com.mitmeerut.CFM_Portal.Model.Heading;
import com.mitmeerut.CFM_Portal.Model.Document;
import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
import com.mitmeerut.CFM_Portal.Repository.HeadingRepository;
import com.mitmeerut.CFM_Portal.Repository.DocumentRepository;
import com.mitmeerut.CFM_Portal.dto.DiffNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VersionComparisonService {

    private final HeadingRepository headingRepo;
    private final DocumentRepository documentRepo;
    private final CourseFileRepository courseFileRepo;

    @Autowired
    public VersionComparisonService(HeadingRepository headingRepo, DocumentRepository documentRepo,
            CourseFileRepository courseFileRepo) {
        this.headingRepo = headingRepo;
        this.documentRepo = documentRepo;
        this.courseFileRepo = courseFileRepo;
    }

    public List<DiffNode> compareVersions(Long v1Id, Long v2Id) {
        List<Heading> h1 = headingRepo.findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(v1Id);
        List<Heading> h2 = headingRepo.findByCourseFileIdAndParentHeadingIsNullOrderByOrderIndexAsc(v2Id);

        return processHeadings(h1, h2);
    }

    private List<DiffNode> processHeadings(List<Heading> h1, List<Heading> h2) {
        Map<String, Heading> map1 = h1.stream().collect(Collectors.toMap(Heading::getTitle, h -> h, (a, b) -> a));
        Map<String, Heading> map2 = h2.stream().collect(Collectors.toMap(Heading::getTitle, h -> h, (a, b) -> a));

        Set<String> allTitles = new LinkedHashSet<>();
        h1.forEach(h -> allTitles.add(h.getTitle()));
        h2.forEach(h -> allTitles.add(h.getTitle()));

        List<DiffNode> nodes = new ArrayList<>();
        for (String title : allTitles) {
            Heading head1 = map1.get(title);
            Heading head2 = map2.get(title);

            DiffNode node = new DiffNode();
            node.setTitle(title);
            node.setNodeType("HEADING");

            if (head1 != null && head2 == null) {
                node.setDiffStatus("REMOVED");
                node.setOldValue(title);
                node.setChildren(processHeadings(getChildHeadings(head1), new ArrayList<>()));
                node.setDocuments(processDocuments(getDocuments(head1), new ArrayList<>()));
            } else if (head1 == null && head2 != null) {
                node.setDiffStatus("ADDED");
                node.setNewValue(title);
                node.setChildren(processHeadings(new ArrayList<>(), getChildHeadings(head2)));
                node.setDocuments(processDocuments(new ArrayList<>(), getDocuments(head2)));
            } else {
                node.setDiffStatus("UNCHANGED");
                node.setChildren(processHeadings(getChildHeadings(head1), getChildHeadings(head2)));
                node.setDocuments(processDocuments(getDocuments(head1), getDocuments(head2)));

                // If any child is modified, we might want to flag this node, but
                // UNCHANGED here means the title itself is the same.
                boolean childrenChanged = node.getChildren().stream()
                        .anyMatch(c -> !c.getDiffStatus().equals("UNCHANGED"));
                boolean docsChanged = node.getDocuments().stream()
                        .anyMatch(d -> !d.getDiffStatus().equals("UNCHANGED"));
                if (childrenChanged || docsChanged) {
                    node.setDiffStatus("MODIFIED");
                }
            }
            nodes.add(node);
        }
        return nodes;
    }

    private List<DiffNode> processDocuments(List<Document> d1, List<Document> d2) {
        // Only consider active documents
        d1 = d1.stream().filter(d -> Boolean.TRUE.equals(d.getIsActive())).collect(Collectors.toList());
        d2 = d2.stream().filter(d -> Boolean.TRUE.equals(d.getIsActive())).collect(Collectors.toList());

        Map<String, Document> map1 = d1.stream().collect(Collectors.toMap(Document::getFileName, d -> d, (a, b) -> a));
        Map<String, Document> map2 = d2.stream().collect(Collectors.toMap(Document::getFileName, d -> d, (a, b) -> a));

        Set<String> allFiles = new LinkedHashSet<>();
        d1.forEach(d -> allFiles.add(d.getFileName()));
        d2.forEach(d -> allFiles.add(d.getFileName()));

        List<DiffNode> nodes = new ArrayList<>();
        for (String fileName : allFiles) {
            Document doc1 = map1.get(fileName);
            Document doc2 = map2.get(fileName);

            DiffNode node = new DiffNode();
            node.setTitle(fileName);
            node.setNodeType("DOCUMENT");

            if (doc1 != null && doc2 == null) {
                node.setDiffStatus("REMOVED");
                node.setOldValue(fileName);
            } else if (doc1 == null && doc2 != null) {
                node.setDiffStatus("ADDED");
                node.setNewValue(fileName);
            } else {
                node.setDiffStatus("UNCHANGED");
            }
            nodes.add(node);
        }
        return nodes;
    }

    private List<Heading> getChildHeadings(Heading h) {
        return headingRepo.findByParentHeadingIdOrderByOrderIndexAsc(h.getId());
    }

    private List<Document> getDocuments(Heading h) {
        return documentRepo.findByHeading_Id(h.getId());
    }
}
