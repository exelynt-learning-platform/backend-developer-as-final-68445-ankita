package com.exelynt.booking.service;

import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    public Resource createResource(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setPricePerHour(request.getPricePerHour());
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long id, ResourceRequest request) {
        Resource existing = getResourceById(id);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPricePerHour(request.getPricePerHour());
        return resourceRepository.save(existing);
    }

    public void deleteResource(Long id) {
        Resource existing = getResourceById(id);
        resourceRepository.delete(existing);
    }
}