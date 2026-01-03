package edu.hm.ccwi.matilda.persistence.jpa.util;

import edu.hm.ccwi.matilda.base.model.library.Library;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MappingLibraryEntityService {

    private static final Logger LOG = LoggerFactory.getLogger(MappingLibraryEntityService.class);

    public Library mapLibraryEntityToLibrary(LibraryEntity le) {
        if(le == null) {
            LOG.debug("mapLibraryEntityToLibrary - libraryentity was null.");
            return null;
        } else {
            return new Library(le.getGroupArtifactId(), le.getGroupId(), le.getArtifactId(), le.getCategory(), le.getTags());
        }
    }

    public LibraryEntity mapLibraryToLibraryEntity(Library li) throws Exception {
        if(li == null) {
            throw new Exception("mapLibraryToLibraryEntity - library is null and therefore cannot be mapped to entity");
        }
        return new LibraryEntity(li.getGroupArtifactId(), li.getGroupId(), li.getArtifactId(), li.getCategory(), li.getTags());
    }

}
