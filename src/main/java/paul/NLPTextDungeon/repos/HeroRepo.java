package paul.NLPTextDungeon.repos;

import org.springframework.data.repository.CrudRepository;
import paul.NLPTextDungeon.entities.Hero;

public interface HeroRepo extends CrudRepository<Hero, Integer> {
}
