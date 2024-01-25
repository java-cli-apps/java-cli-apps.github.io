package fr;

import java.util.Optional;

import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;

public class Hello {
    public static void main() {
        Optional<Emoji> optionalEmoji = EmojiManager.getByAlias("fr");
        String emoji = optionalEmoji.isPresent() ? " " + optionalEmoji.get().getEmoji() : "";
        System.out.println("Bonjour" + emoji);
    }
}