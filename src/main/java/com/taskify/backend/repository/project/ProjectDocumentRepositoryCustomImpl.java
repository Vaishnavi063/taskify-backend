package com.taskify.backend.repository.project;

import com.taskify.backend.constants.DocumentEnums.DocAccessType;
import com.taskify.backend.constants.DocumentEnums.DocStatus;
import com.taskify.backend.dto.Document.GetDocumentsQueryDto;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ProjectDocumentRepositoryCustomImpl implements ProjectDocumentRepositoryCustom {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Optional<Map<String, Object>> getFullDocumentById(String docId, String memberId) {
        // Validate ObjectId format
        if (docId == null || !ObjectId.isValid(docId)) {
            return Optional.empty();
        }
        
        try {
            // Use simple find and manual population for now
            Criteria criteria = Criteria.where("_id").is(new ObjectId(docId)).and("isDeleted").is(false);
            
            List<Map> results = mongoTemplate.find(
                org.springframework.data.mongodb.core.query.Query.query(criteria), 
                Map.class, 
                "documents"
            );
            
            if (results.isEmpty()) {
                return Optional.empty();
            }
            
            Map<String, Object> doc = results.get(0);
            
            // Convert ObjectId to string
            if (doc.get("_id") instanceof ObjectId) {
                doc.put("_id", ((ObjectId) doc.get("_id")).toString());
            }
            
            // Add computed fields
            doc.put("isCreator", doc.get("memberId").equals(memberId));
            doc.put("isMember", false); // Simplified for now
            doc.put("members", new ArrayList<>());
            doc.put("comments", new ArrayList<>());
            doc.put("creator", Map.of(
                "fullName", "User Name",
                "email", "user@example.com",
                "role", "Owner",
                "avatar", "avatar_url"
            ));
            
            return Optional.of(doc);
            
        } catch (Exception e) {
            System.err.println("Error in getFullDocumentById: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    @Override
    public Map<String, Object> getDocuments(String projectId, String memberId, GetDocumentsQueryDto filters) {
        try {
            System.out.println("Getting documents for projectId: " + projectId + ", memberId: " + memberId);
            
            Criteria criteria = Criteria.where("projectId").is(projectId).and("isDeleted").is(false);
            
            // Apply filters
            if (filters.getTitle() != null && !filters.getTitle().isEmpty()) {
                criteria.and("title").regex(filters.getTitle(), "i");
            }
            
            if (filters.getCreatedByMe() != null && filters.getCreatedByMe()) {
                criteria.and("memberId").is(memberId);
            }
            
            if (filters.getAssignedToMe() != null && filters.getAssignedToMe()) {
                criteria.and("assignees").in(memberId);
                criteria.and("status").is(DocStatus.PUBLISHED);
            }
            
            if (filters.getStatus() != null) {
                criteria.and("status").is(filters.getStatus());
            }
            
            if (filters.getIsPublic() != null && filters.getIsPublic()) {
                criteria.and("accessType").is(DocAccessType.PUBLIC);
                criteria.and("status").is(DocStatus.PUBLISHED);
            }
            
            // Use simple find query first
            org.springframework.data.mongodb.core.query.Query query = 
                org.springframework.data.mongodb.core.query.Query.query(criteria);
            
            // Add sorting
            if (filters.getSortByCreated() != null && filters.getSortByCreated()) {
                query.with(Sort.by(Sort.Direction.ASC, "createdAt"));
            } else {
                query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
            
            List<Map> docs = mongoTemplate.find(query, Map.class, "documents");
            
            System.out.println("Found " + docs.size() + " documents");
            
            // Process each document
            List<Map<String, Object>> processedDocs = new ArrayList<>();
            for (Map doc : docs) {
                Map<String, Object> processedDoc = new HashMap<>(doc);
                
                // Convert ObjectId to string
                if (processedDoc.get("_id") instanceof ObjectId) {
                    processedDoc.put("_id", ((ObjectId) processedDoc.get("_id")).toString());
                }
                
                // Add computed fields
                processedDoc.put("commentCount", 
                    processedDoc.get("comments") != null ? 
                    ((List<?>) processedDoc.get("comments")).size() : 0);
                
                processedDoc.put("isMember", false); // Simplified
                processedDoc.put("members", new ArrayList<>());
                
                // Add creator info (simplified)
                processedDoc.put("creator", Map.of(
                    "memberId", processedDoc.get("memberId"),
                    "email", "user@example.com",
                    "role", "Owner",
                    "fullName", "User Name",
                    "avatar", Map.of("url", "avatar_url", "_id", "avatar_id")
                ));
                
                processedDocs.add(processedDoc);
            }
            
            // Calculate pagination
            int page = filters.getPage() != null ? filters.getPage() : 1;
            int limit = filters.getLimit() != null ? filters.getLimit() : 10;
            int total = processedDocs.size();
            int totalPages = (int) Math.ceil((double) total / limit);
            
            // Apply pagination
            int startIndex = (page - 1) * limit;
            int endIndex = Math.min(startIndex + limit, total);
            List<Map<String, Object>> paginatedDocs = processedDocs.subList(startIndex, endIndex);
            
            // Build response matching Node.js format
            Map<String, Object> response = new HashMap<>();
            response.put("docs", paginatedDocs);
            response.put("total", total);
            response.put("limit", limit);
            response.put("page", page);
            response.put("totalPages", totalPages);
            response.put("serialNumberStartFrom", startIndex + 1);
            response.put("hasPrevPage", page > 1);
            response.put("hasNextPage", page < totalPages);
            response.put("prevPage", page > 1 ? page - 1 : null);
            response.put("nextPage", page < totalPages ? page + 1 : null);
            
            System.out.println("Returning response with " + paginatedDocs.size() + " docs");
            return response;
            
        } catch (Exception e) {
            System.err.println("Error in getDocuments: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch documents: " + e.getMessage(), e);
        }
    }
}