package mcia.publications.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import mcia.publications.domain.Publication;
import mcia.publications.domain.Publisher;
import mcia.publications.repository.PublicationRepository;
import mcia.publications.repository.PublisherRepository;

@RestController
@RequestMapping("/api/publications")
@Slf4j
public class PublicationController {

	private static final int PAGE_SIZE = 10;
	private static final String AFTER = "1990";
	private static final String BEFORE = "3000";

	@Autowired
	PublicationRepository publicationRepository;

	@Autowired
	PublisherRepository publisherRepository;

	@RequestMapping(value = "", method = RequestMethod.GET)
	public Page<Publication> query(
			@RequestParam(name = "q", defaultValue = "") String query,
			@RequestParam(name = "author", required = false) String authorId,
			@RequestParam(name = "type", defaultValue = "all") String type,
			@RequestParam(name = "after", defaultValue = AFTER) @DateTimeFormat(pattern = "yyyy") Date after,
			@RequestParam(name = "before", defaultValue = BEFORE) @DateTimeFormat(pattern = "yyyy") Date before,
			@RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page) {
		log.info("GET: publications page={}, q={}, author={}, type={}, after={}, before={}",
				page, query, authorId, type, after, before);

		// Fetch publishers matching type
		List<String> publisherIds;
		if (!type.equalsIgnoreCase("all")) {
			List<Publisher> publishers = publisherRepository.findByType(type);
			publisherIds = publishers.stream().map(Publisher::getId).collect(Collectors.toList());
			log.debug("Found {} publishers matching type={}", publisherIds.size(), type);
		} else {
			publisherIds = Collections.emptyList();
		}

		// Run search
		Page<Publication> result = publicationRepository.search(
				query, authorId, publisherIds, after, before, new PageRequest(page, PAGE_SIZE));
		log.info("Matched {} elements in {} pages", result.getTotalElements(), result.getTotalPages());
		return result;
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public Publication getById(@PathVariable String id) {
		log.info("GET: publication by id={}", id);
		return publicationRepository.findOne(id);
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	public Publication post(@RequestBody Publication publication) {
		log.info("POST: {}", publication);
		Publication saved = null;
		if (publication.getId() != null) {
			throw new RuntimeException("insert of new publication must not provide an id");
		} else {
			saved = publicationRepository.save(publication);
			log.info("Created publication id={}, title={}", saved.getId(), saved.getTitle());
		}
		return saved;
	}

	@RequestMapping(value = "", method = RequestMethod.PUT)
	public Publication put(@RequestBody Publication publication) {
		log.info("PUT: {}", publication);
		Publication saved = null;
		if (publication.getId() != null) {
			if (publicationRepository.exists(publication.getId())) {
				saved = publicationRepository.save(publication);
				log.info("Updated publication id={}, title={}", saved.getId(), saved.getTitle());
			} else {
				throw new RuntimeException("cannot update publication with unknown id=" + publication.getId());
			}
		} else {
			throw new RuntimeException("cannot update publication with undefined id");
		}
		return saved;
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void deletebyId(@PathVariable String id) {
		log.info("DELETE: publication by id={}", id);
		publicationRepository.delete(id);
	}

}
