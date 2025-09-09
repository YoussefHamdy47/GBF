import { ColorResolvable, EmbedBuilder, Guild, User } from "discord.js";
import { ColorCodes, GetRandomFromArray } from "../../Handler";
import { EightBallAnswers, TopicQuestions } from "../Data/Fun Arrays";

export class GBFFun {
  static EightBall(
    UserQuestion: string,
    ReturnEmbed: boolean,
    EmbedColor: ColorResolvable = ColorCodes.Default
  ): string | EmbedBuilder {
    const Result = GetRandomFromArray(EightBallAnswers);

    const EightBallEmbed = new EmbedBuilder()
      .setTitle("🎱 Magic 8Ball")
      .setDescription(
        `**Question »** ${UserQuestion} \n **Answer »** ${Result}`
      )
      .setColor(EmbedColor);

    return ReturnEmbed ? EightBallEmbed : Result;
  }

  static TopicGenerate(EmbedColor: ColorResolvable = ColorCodes.Default): EmbedBuilder {
    const TopicQuestion = GetRandomFromArray(TopicQuestions);

    const TopicEmbed = new EmbedBuilder()
      .setTitle("Random Topic")
      .addFields({
        name: "━━━━━━━━━━━━━━━━━━",
        value: TopicQuestion,
      })
      .setColor(EmbedColor);

    return TopicEmbed;
  }

  static Kill(TargetUser: User, CommandUser: User, Guild?: Guild) {
    let TargetDisplayName: string = TargetUser.username;
    let UserDisplayName: string = CommandUser.username;

    if (Guild) {
      const TargetMember = Guild.members.cache.get(TargetUser.id);
      TargetDisplayName = TargetMember
        ? TargetMember.nickname
        : TargetUser.username;
      const CommandMember = Guild.members.cache.get(CommandUser.id);
      UserDisplayName = CommandMember
        ? CommandMember.nickname
        : CommandUser.username;
    }

    const KillDialogue = [
      `${TargetDisplayName} watched a female comedian.`,
      `Jett couldn't revive ${TargetDisplayName}.`,
      `${TargetDisplayName}'s elytra broke.`,
      `${TargetDisplayName} forgot their water bucket.`,
      `${TargetDisplayName} bullied the quiet kid.`,
      `${TargetDisplayName} fought the blue-haired girl.`,
      `${TargetDisplayName} had America's oil.`,
      `${TargetDisplayName} found the cure for cancer, but the next day they magically disappeared.`,
      `${TargetDisplayName} cancelled their subscription for living.`,
      `${TargetDisplayName} died from AIDS...`,
      `${TargetDisplayName} died waiting for GBF to have good commands.`,
      `${TargetDisplayName} was eaten by the Duolingo owl...`,
      `${TargetDisplayName} killed their snapstreak with ${UserDisplayName}, causing ${UserDisplayName} to get really angry at them, then they shot them twice.`,
      `${TargetDisplayName} missed their Duolingo Spanish lessons...`,
      `${TargetDisplayName} died from a broken heart after being rejected by their crush, ${UserDisplayName}.`,
      `${TargetDisplayName} got dunked on by a Fortnite kid cranking 90s.`,
      `${TargetDisplayName} choked on their own saliva.`,
      `${TargetDisplayName} died from a botched boob job.`,
      `${TargetDisplayName} was stabbed by ${UserDisplayName} after they called their mom fat.`,
      `${UserDisplayName} dropped a Nokia phone on ${TargetDisplayName}.`,
      `${TargetDisplayName} choked on... water.`,
      `${TargetDisplayName} died from loneliness.`,
      `${TargetDisplayName} got dabbed on for being a hater.`,
      `${TargetDisplayName} tripped on nothing and died.`,
      `${TargetDisplayName} killed themselves after ${UserDisplayName} showed them some unfunny memes.`,
      `${UserDisplayName} tried to kill ${TargetDisplayName} but failed.`,
      `${TargetDisplayName} used bots in general.`,
      `${TargetDisplayName} sent NSFW in general!`,
      `${TargetDisplayName} talked back to their mom.`,
      `${TargetDisplayName} said a no-no word in a Christian Minecraft server.`,
      `${TargetDisplayName} got a stroke after watching Jake Paul.`,
      `${TargetDisplayName} killed themselves after getting cheated on by ${UserDisplayName}.`,
      `${TargetDisplayName} was blown up by a creeper.`,
      `${UserDisplayName} tried to kill ${TargetDisplayName} but ${TargetDisplayName} shot ${UserDisplayName} twice.`,
      `${TargetDisplayName} was run over by ${UserDisplayName}.`,
      `${TargetDisplayName} got into an argument with an angry feminist.`,
      `${TargetDisplayName} default danced to death.`,
      `${TargetDisplayName} drowned.`,
      `${TargetDisplayName} drowned after being pushed into the water by ${UserDisplayName}.`,
      `${TargetDisplayName} joined engineering.`,
      `${TargetDisplayName} tried to study Linear Algebra.`,
      `${TargetDisplayName} tried going to med school.`,
      `${TargetDisplayName} chose the easy way out.`,
      `${TargetDisplayName} might not come back.`,
      `${TargetDisplayName} got dunked on.`,
    ];

    const KillMessage = GetRandomFromArray(KillDialogue);

    return KillMessage;
  }
}
