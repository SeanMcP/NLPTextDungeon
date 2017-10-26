package paul.NLPTextDungeon.repos;

import org.springframework.data.repository.CrudRepository;
import paul.NLPTextDungeon.entities.Hero;

import java.util.List;

public interface HeroRepo extends CrudRepository<Hero, Integer> {
    Hero findFirstByName (String name);
    List<Hero> findByLevelGreaterThan (int level);
}
