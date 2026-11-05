package fun.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 놀이 명령어(dice, pick, ladder, conch) 테스트 클래스
 */
class FunCommandsTest {

    private SlashCommandInteractionEvent event;
    private ReplyCallbackAction replyAction;

    @BeforeEach
    void setUp() {
        event = mock(SlashCommandInteractionEvent.class);
        replyAction = mock(ReplyCallbackAction.class);
        when(event.reply(anyString())).thenReturn(replyAction);
        when(replyAction.setEphemeral(anyBoolean())).thenReturn(replyAction);
    }

    private void givenOption(String name, String value) {
        OptionMapping option = mock(OptionMapping.class);
        when(option.getAsString()).thenReturn(value);
        when(event.getOption(name)).thenReturn(option);
    }

    // 항상 같은 값을 돌려주는 Random (테스트용)
    private static Random fixedRandom(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value % bound;
            }
        };
    }

    @Nested
    @DisplayName("/dice")
    class Dice {

        @Test
        @DisplayName("1부터 면 수 사이의 값을 개수만큼 굴린다")
        void shouldRollWithinRange() {
            // given
            DiceCommand dice = new DiceCommand(new Random(42));

            // when
            List<Integer> rolls = dice.roll(6, 10);

            // then
            assertEquals(10, rolls.size());
            assertTrue(rolls.stream().allMatch(value -> value >= 1 && value <= 6));
        }

        @Test
        @DisplayName("한 개면 값만, 여러 개면 합계도 보여준다")
        void shouldFormatRolls() {
            assertEquals("🎲 **4** (1~6)", DiceCommand.format(List.of(4), 6));
            assertEquals("🎲 3, 5 → 합계 **8** (1~20 × 2개)", DiceCommand.format(List.of(3, 5), 20));
        }

        @Test
        @DisplayName("명령어 옵션에 면 수와 개수 범위가 걸려 있다")
        void shouldLimitOptionRanges() {
            var options = new DiceCommand().data().getOptions();
            assertEquals(2L, options.get(0).getMinValue().longValue());
            assertEquals((long) DiceCommand.MAX_SIDES, options.get(0).getMaxValue().longValue());
            assertEquals((long) DiceCommand.MAX_COUNT, options.get(1).getMaxValue().longValue());
        }
    }

    @Nested
    @DisplayName("/pick")
    class Pick {

        @Test
        @DisplayName("쉼표(전각 쉼표 포함)로 나누고 빈 항목은 뺀다")
        void shouldSplitItems() {
            assertEquals(List.of("짜장면", "짬뽕", "탕수육", "볶음밥"),
                    PickCommand.splitItems(" 짜장면, 짬뽕 ,, 탕수육，볶음밥 "));
        }

        @Test
        @DisplayName("선택지 중 하나를 골라 알려준다")
        void shouldPickOneChoice() {
            // given
            givenOption("choices", "짜장면, 짬뽕, 탕수육");

            // when
            new PickCommand(fixedRandom(1)).execute(event);

            // then
            verify(event).reply("🤔 고민 끝! **짬뽕**\n(후보: 짜장면, 짬뽕, 탕수육)");
        }

        @Test
        @DisplayName("선택지가 2개 미만이면 사용법을 알려준다")
        void shouldRejectSingleChoice() {
            // given
            givenOption("choices", "짜장면");

            // when
            new PickCommand().execute(event);

            // then
            verify(replyAction).setEphemeral(true);
        }
    }

    @Nested
    @DisplayName("/ladder")
    class Ladder {

        @Test
        @DisplayName("결과를 비우면 한 명만 당첨, 나머지는 꽝이다")
        void shouldUseDefaultResults() {
            assertEquals(List.of(LadderCommand.WIN, LadderCommand.LOSE, LadderCommand.LOSE),
                    LadderCommand.defaultResults(3));
        }

        @Test
        @DisplayName("모든 참가자가 결과를 하나씩 받는다")
        void shouldGiveEveryParticipantOneResult() {
            // given
            List<String> participants = List.of("철수", "영희", "민수");
            List<String> results = List.of("커피", "꽝", "청소");

            // when
            Map<String, String> matched = new LadderCommand(new Random(7)).match(participants, results);

            // then
            assertEquals(participants, new ArrayList<>(matched.keySet()));
            List<String> given = new ArrayList<>(matched.values());
            given.sort(null);
            List<String> expected = new ArrayList<>(results);
            expected.sort(null);
            assertEquals(expected, given);
        }

        @Test
        @DisplayName("참가자 수와 결과 수가 다르면 알려준다")
        void shouldRejectMismatchedCounts() {
            assertNotNull(LadderCommand.validate(List.of("철수", "영희"), List.of("꽝")));
            assertNotNull(LadderCommand.validate(List.of("철수"), List.of("꽝")));
            assertNull(LadderCommand.validate(List.of("철수", "영희"), List.of("당첨", "꽝")));
        }

        @Test
        @DisplayName("결과를 참가자 순서대로 보여준다")
        void shouldReplyResults() {
            // given
            givenOption("participants", "철수, 영희");

            // when
            new LadderCommand(fixedRandom(0)).execute(event);

            // then: [당첨, 꽝] → Collections.shuffle(항상 0번과 교환) → [꽝, 당첨]
            verify(event).reply("🪜 **사다리 타기 결과**\n철수 ➜ **꽝**\n영희 ➜ **당첨 🎉**\n");
        }
    }

    @Nested
    @DisplayName("/conch")
    class Conch {

        @Test
        @DisplayName("정해진 대답 중 하나로 답한다")
        void shouldAnswerFromList() {
            // when
            String answer = new ConchCommand(new Random()).answer();

            // then
            assertTrue(ConchCommand.ANSWERS.contains(answer));
        }

        @Test
        @DisplayName("질문과 대답을 함께 보여준다")
        void shouldReplyWithQuestionAndAnswer() {
            // given
            givenOption("question", " 오늘 야식 먹어도 될까? ");

            // when
            new ConchCommand(fixedRandom(1)).execute(event);

            // then
            verify(event).reply("🐚 마법의 소라고둥님, **오늘 야식 먹어도 될까?**\n> 안 돼.");
        }
    }
}
