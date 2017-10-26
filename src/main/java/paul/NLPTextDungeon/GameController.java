package paul.NLPTextDungeon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import paul.NLPTextDungeon.entities.Hero;
import paul.NLPTextDungeon.repos.HeroRepo;
import paul.NLPTextDungeon.utils.DefeatException;
import paul.NLPTextDungeon.parsing.InputType;
import paul.NLPTextDungeon.parsing.TextInterface;
import paul.NLPTextDungeon.utils.VictoryException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Paul Dennis on 8/16/2017.
 */
@Controller
public class GameController {

    @Autowired
    HeroRepo heroes;

    InputType requestedInputType = InputType.STD;

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String home () {
        return "index";
    }

    @RequestMapping(path = "/game", method = RequestMethod.GET)
    public String game (Model model, HttpSession session) throws IOException {
        TextInterface textOut = (TextInterface) session.getAttribute("textInterface");
        if (textOut == null) {
            textOut = new TextInterface();
            textOut.start(null);
            session.setAttribute("textInterface", textOut);
        }
        requestedInputType = textOut.show();//Important

        List<String> output = textOut.flush();
        List<String> debug = textOut.flushDebug();
        if (output.size() == 0) {
            debug.add("There was no content in the buffer");
        }
        List<String> tutorial = textOut.flushTutorial();
        if (tutorial.size() > 0 && tutorial.get(0) == null) {
            tutorial = null;
        }

        model.addAttribute("outputText", output);

        if (tutorial != null && tutorial.size() > 0) {
            System.out.println("tutorial added with size " + tutorial.size());
            model.addAttribute("tutorial", tutorial);
        }

        if (debug != null && debug.size() > 0) {
            model.addAttribute("debugText", debug);
        }
        model.addAttribute("location", textOut.getRunner().getDungeon().getDungeonName());
        model.addAttribute("roomName", "  " + textOut.getRunner().getHero().getLocation().getName());
        session.setAttribute("hero", textOut.getRunner().getHero());
        return "game";
    }

    @RequestMapping(path = "/heroes", method = RequestMethod.GET)
    public String viewHeroes (Model model) {
        List<Hero> heroList = new ArrayList<>();
        heroes.findAll().forEach(heroList::add);
        model.addAttribute("heroes", heroList);
        return "heroes";
    }

    @RequestMapping(path = "/save-hero", method = RequestMethod.GET)
    public String saveHero (HttpSession session) {
        heroes.save((Hero)session.getAttribute("hero"));

        return "redirect:/heroes";
    }

    @RequestMapping(path = "/submit-action", method = RequestMethod.POST)
    public String submitAction (@RequestParam String userInput, Model model, HttpSession session) {
        TextInterface textOut = (TextInterface) session.getAttribute("textInterface");

        if (requestedInputType == InputType.NUMBER) {
            try {
                Integer.parseInt(userInput);
            } catch (NumberFormatException ex) {
                textOut.debug("You broke it by not entering a number. Thanks");
            }
        }
        if (!userInput.equals("")) {
            textOut.println("You entered: \"" + userInput + "\"");
        }
        try {
            textOut.processResponse(userInput);
        } catch (DefeatException ex) {
            textOut.println(ex.getMessage());
            textOut.println("You died. GAME OVER.");
        } catch (VictoryException ex) {
            textOut.println(ex.getMessage());
            textOut.println("You won! Awesome!");
        }
        return "redirect:/game";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleError(HttpServletRequest req, Exception ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", ex);
        mav.addObject("stackTrace", ex.getStackTrace());
        mav.setViewName("error");
        ex.printStackTrace();
        return mav; //Queen of Air and Darkness
    }
}
