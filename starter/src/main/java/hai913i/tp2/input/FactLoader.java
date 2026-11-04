package hai913i.tp2.input;

import java.io.IOException;
import java.nio.file.Path;
import hai913i.tp2.model.FactDataset;

public interface FactLoader {
    FactDataset loadFacts(Path inputPath) throws IOException;
}