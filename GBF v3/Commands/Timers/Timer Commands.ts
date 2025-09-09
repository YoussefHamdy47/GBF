import {
  ActionRowBuilder,
  ApplicationCommandOptionType,
  ButtonBuilder,
  ButtonStyle,
  ChatInputCommandInteraction,
  CommandInteractionOptionResolver,
  ComponentType,
  EmbedBuilder,
  Interaction,
  MessageFlags,
  type User,
} from "discord.js";
import {
  SlashCommand,
  GBF,
  ColorCodes,
  msToTime,
  secondsToHours,
  Emojis,
} from "../../Handler";
import { Grade, Subject } from "../../GBF/Timers/GradeEngine";
import { UserModel } from "../../Models/User/UserModel";
import { TimerModel } from "../../Models/Timer/TimerModel";
import {
  createTimerActionRow,
  TimerButtonID,
} from "../../GBF/Timers/TimerHelper";
import { Timers } from "../../GBF/Timers/Timers";

function randomWelcome(user: User) {
  const welcomeMessages = [
    `Welcome, ${user.username}!`,
    `Hello, ${user.username}!`,
    `Hey there, ${user.username}!`,
    `Yo, ${user.username}!`,
    `Nice to see you, ${user.username}!`,
    `Ahoy, ${user.username}!`,
    `Good to have you here, ${user.username}!`,
    `Hey hey, ${user.username}!`,
    `Glad you’re here, ${user.username}!`,
    `What’s up, ${user.username}!`,
  ];

  return welcomeMessages[Math.floor(Math.random() * welcomeMessages.length)];
}

export class GBFTimers extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "timer",
      description: "Track your study time and progress",
      category: "Timers",
      subcommands: {
        stats: {
          description: "View your study stats",
          SubCommandOptions: [
            {
              name: "ephemeral",
              description: "Whether the message should be ephemeral",
              type: ApplicationCommandOptionType.Boolean,
            },
          ],
          async execute({ client, interaction }) {
            const ephemeral =
              (
                (interaction).options as CommandInteractionOptionResolver
              ).getBoolean("ephemeral") ?? false;

            const statsEmbed = new EmbedBuilder();

            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              const stats = timers.statDisplay();

              statsEmbed
                .setColor(ColorCodes.Default)
                .setTitle(
                  `${randomWelcome(
                    interaction.user
                  )} - ${timers.getSemesterName()}`
                )
                .setDescription(stats)
                .setFooter({
                  text: `Stats for ${interaction.user.username}`,
                })
                .setTimestamp();

              return interaction.reply({
                embeds: [statsEmbed],
                flags: ephemeral ? MessageFlags.Ephemeral : undefined,
              });
            } catch (error) {
              statsEmbed
                .setColor(ColorCodes.ErrorRed)
                .setDescription(
                  `I ran into an error while trying to fetch your stats. Please try again later.\n\n\`\`\`js\n` +
                    error +
                    `\`\`\``
                );

              return interaction.reply({
                embeds: [statsEmbed],
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        ["add-subject"]: {
          description: "Add a subject your account / current semester",
          SubCommandOptions: [
            {
              name: "type",
              description: "The account type to add the subject too",
              type: ApplicationCommandOptionType.String,
              choices: [
                {
                  name: "Account",
                  value: "A",
                },
                {
                  name: "Semester",
                  value: "S",
                },
              ],
              required: true,
            },
            {
              name: "subject-name",
              description: "The name of the subject",
              type: ApplicationCommandOptionType.String,
              maxLength: 50,
              required: true,
            },
            {
              name: "subject-code",
              description: "The subject's code",
              type: ApplicationCommandOptionType.String,
              maxLength: 50,
              required: true,
            },
            {
              name: "credits",
              description: "The number of credits for the subject",
              type: ApplicationCommandOptionType.Integer,
              required: true,
              minValue: 1,
              maxValue: 4,
            },
            {
              name: "grade",
              description:
                "The grade achieved in the subject, only choose if type is account",
              type: ApplicationCommandOptionType.String,
              choices: [
                { name: "A+", value: Grade.A_PLUS },
                { name: "A", value: Grade.A },
                { name: "A-", value: Grade.A_MINUS },
                { name: "B+", value: Grade.B_PLUS },
                { name: "B", value: Grade.B },
                { name: "B-", value: Grade.B_MINUS },
                { name: "C+", value: Grade.C_PLUS },
                { name: "C", value: Grade.C },
                { name: "C-", value: Grade.C_MINUS },
                { name: "D+", value: Grade.D_PLUS },
                { name: "D", value: Grade.D },
                { name: "F", value: Grade.F },
                { name: "W", value: Grade.W },
                { name: "P", value: Grade.P },
              ],
            },
          ],
          async execute({ client, interaction }) {
            const subjectName = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("subject-name", true);

            const subjectCode = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("subject-code", true);

            const credits = (
              interaction.options as CommandInteractionOptionResolver
            ).getInteger("credits", true);

            const accountType = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("type", true);

            const grade =
              (
                interaction.options as CommandInteractionOptionResolver
              ).getString("grade") ?? null;

            if (accountType === "A" && !grade)
              return interaction.reply({
                content:
                  "If you want to add a subject to your account, you must specify the grade achieved.",
                flags: MessageFlags.Ephemeral,
              });

            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              const newSubject: Subject = {
                subjectName: subjectName,
                subjectCode: subjectCode,
                creditHours: credits,
                timesStudied: 0,
                grade: accountType === "A" ? (grade as Grade) : undefined,
                marksLost: 0,
              };

              if (accountType === "S")
                await timers.addSubjectSemester(newSubject);

              if (accountType === "A")
                await timers.addSubjectAccount(newSubject);

              return interaction.reply({
                content: `Subject '${subjectName}' has been added to ${
                  accountType === "S" ? "the current Semester" : "your account"
                }, with ${credits} credits.\nBest of luck.`,
                flags: MessageFlags.Ephemeral,
              });
            } catch (error) {
              return interaction.reply({
                content: error.message,
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        ["remove-subject"]: {
          description: "Remove a subject from the current semester",
          SubCommandOptions: [
            {
              name: "type",
              description: "The account type to add the subject too",
              type: ApplicationCommandOptionType.String,
              choices: [
                {
                  name: "Account",
                  value: "A",
                },
                {
                  name: "Semester",
                  value: "S",
                },
              ],
              required: true,
            },
            {
              name: "subject-code",
              description: "The code of the subject",
              type: ApplicationCommandOptionType.String,
              autocomplete: true,
              required: true,
            },
          ],
          async autocomplete(interaction, option) {
            const userData = await UserModel.findOne({
              userID: interaction.user.id,
            });

            const timerData = await TimerModel.findOne({
              "account.userID": interaction.user.id,
            });

            if (!userData) return ["NA"];

            let subjectCodes = userData.Subjects.map(
              (subject) => `${subject.subjectCode} - ${subject.subjectName}`
            );

            if (
              !timerData ||
              !timerData.currentSemester.semesterSubjects.length
            )
              return subjectCodes;

            if (timerData?.currentSemester?.semesterSubjects?.length) {
              subjectCodes = [
                ...subjectCodes,
                ...timerData.currentSemester.semesterSubjects.map(
                  (subject) => `${subject.subjectCode} - ${subject.subjectName}`
                ),
              ];
            }

            return subjectCodes;
          },
          async execute({ client, interaction }) {
            const type = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("type");

            const chosenSubject = (
              interaction.options as CommandInteractionOptionResolver
            )
              .getString("subject-code")
              ?.split(" - ")[0];

            if (chosenSubject === "NA")
              return interaction.reply({
                content: "You don't have any subjects added to your account.",
                flags: MessageFlags.Ephemeral,
              });

            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              if (type === "A")
                await timers.removeSubjectAccount(chosenSubject);

              if (type === "S")
                await timers.removeSubjectSemester(chosenSubject);

              return interaction.reply({
                content: `Removed ${chosenSubject}`,
                flags: MessageFlags.Ephemeral,
              });
            } catch (error) {
              return interaction.reply({
                content: error.message,
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        register: {
          description: "Register a new semester",
          SubCommandOptions: [
            {
              name: "semester-name",
              description: "The name of the semester",
              type: ApplicationCommandOptionType.String,
              maxLength: 50,
              required: true,
            },
          ],
          async execute({ client, interaction }) {
            const semesterName = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("semester-name", true);

            const registerEmbed = new EmbedBuilder();

            try {
              const timer = await Timers.create(interaction.user.id);

              const register = await timer.register(semesterName);

              if (register) {
                registerEmbed
                  .setTitle(
                    `Welcome ${interaction.user.username} to GBF Timers!`
                  )
                  .setColor(ColorCodes.Default)
                  .setDescription(
                    `Successfully registered a new Semester '${semesterName}', best of luck.`
                  )
                  .setTimestamp();

                return interaction.reply({
                  embeds: [registerEmbed],
                });
              } else {
                throw new Error("Couldn't register, please try again later.");
              }
            } catch (error) {
              registerEmbed
                .setColor(ColorCodes.ErrorRed)
                .setDescription(
                  `I ran into an error while trying to register you. Please try again later.\n\n\`\`\`js\n` +
                    error +
                    `\`\`\``
                )
                .setTimestamp()
                .setFooter({
                  text: "Error registering user",
                });

              return interaction.reply({
                embeds: [registerEmbed],
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        gpa: {
          description:
            "Returns a list of all of the subjects taken with their grade",
          async execute({ client, interaction }) {
            const subjectsEmbed = new EmbedBuilder();

            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              const subjects = timers.GPAMenu();

              subjectsEmbed
                .setTitle(`${interaction.user.username}'s GPA`)
                .setColor(ColorCodes.Default)
                .setDescription(subjects)
                .setTimestamp();

              return interaction.reply({
                embeds: [subjectsEmbed],
                flags: MessageFlags.Ephemeral,
              });
            } catch (error) {
              subjectsEmbed
                .setColor(ColorCodes.ErrorRed)
                .setDescription(
                  `I ran into an error while trying to fetch your stats. Please try again later.\n\n\`\`\`js\n` +
                    error +
                    `\`\`\``
                )
                .setTimestamp()
                .setFooter({
                  text: "Error registering user",
                });

              return interaction.reply({
                embeds: [subjectsEmbed],
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        start: {
          description: "Start a study session",
          SubCommandOptions: [
            {
              name: "subject",
              description: "The subject to be studied",
              type: ApplicationCommandOptionType.String,
              required: true,
              autocomplete: true,
            },
          ],
          async autocomplete(interaction, option) {
            const timerData = await TimerModel.findOne({
              "account.userID": interaction.user.id,
            });

            if (
              !timerData ||
              !timerData.currentSemester.semesterSubjects.length
            )
              return ["NA"];

            return timerData.currentSemester.semesterSubjects.map(
              (subject) => `${subject.subjectCode} - ${subject.subjectName}`
            );
          },
          async execute({ client, interaction }) {
            const chosenSubject = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("subject");

            if (chosenSubject === "NA")
              return interaction.reply({
                content:
                  "You don't have any subjects registered this semester.",
                flags: MessageFlags.Ephemeral,
              });

            const splitSubject = chosenSubject?.split(" - ")[0].trim();

            const startEmbed = new EmbedBuilder();

            try {
              const timers = await Timers.create(
                interaction.user.id,
                true,
                false,
                interaction,
                client
              );

              await interaction.deferReply({
                withResponse: true,
              });

              let startDescription = `• Semester Study Time: ${
                timers.timerStats.getSemesterTime() !== 0
                  ? msToTime(timers.timerStats.getSemesterTime()) +
                    ` [${secondsToHours(
                      timers.timerStats.getSemesterTime() / 1000
                    )}]`
                  : "0s"
              }\n`;

              startDescription += `• Average Session Time: ${
                timers.timerStats.getAverageSessionTime() !== 0
                  ? msToTime(timers.timerStats.getAverageSessionTime())
                  : "0s"
              }\n`;

              startDescription += `• Total Sessions: ${timers.timerStats.getSessionCount()}\n\n`;

              startDescription += `• Semester Break Time: ${
                timers.timerStats.getBreakTime() !== 0
                  ? msToTime(timers.timerStats.getBreakTime())
                  : "0s"
              }\n`;

              startDescription += `• Total Breaks: ${timers.timerStats.getBreakCount()}\n`;

              startDescription += `• Average Break Time: ${
                timers.timerStats.getAverageBreakTime() !== 0
                  ? msToTime(timers.timerStats.getAverageBreakTime())
                  : "0s"
              }\n\n`;

              const subjectDB =
                timers.timerData.currentSemester.semesterSubjects.find(
                  (subject) =>
                    subject.subjectCode.trim().toLowerCase() ===
                    splitSubject.toLowerCase()
                );

              startDescription += `• Times Studied ${chosenSubject}: ${subjectDB.timesStudied}`;

              startEmbed
                .setTitle(
                  `${randomWelcome(interaction.user)} | ${chosenSubject}`
                )
                .setDescription(startDescription)
                .setColor(ColorCodes.Default)
                .setTimestamp();

              const actionRow = createTimerActionRow({
                [TimerButtonID.Pause]: true,
                [TimerButtonID.Info]: true,
                [TimerButtonID.Stop]: true,
              });

              const sessionStartMessage = await interaction.editReply({
                embeds: [startEmbed],
                components: [actionRow],
              });

              timers.timerData.sessionData.guildID = interaction.guildId;
              timers.timerData.sessionData.messageID = sessionStartMessage.id;
              timers.timerData.sessionData.channelID = interaction.channelId;
              timers.timerData.sessionData.sessionTopic = chosenSubject;

              await timers.timerData!.save();
            } catch (error) {
              startEmbed
                .setTitle("I ran into an error :(")
                .setColor(ColorCodes.ErrorRed)
                .setDescription(
                  `I ran into an error while trying to start a session. Please try again later.\n\n\`\`\`js\n` +
                    error +
                    `\`\`\``
                );

              return interaction.editReply({
                embeds: [startEmbed],
              });
            }
          },
        },
        ["change-subject"]: {
          description: "Change the subject of the current session",
          SubCommandOptions: [
            {
              name: "subject",
              description: "The subject to be studied",
              type: ApplicationCommandOptionType.String,
              required: true,
              autocomplete: true,
            },
          ],
          async autocomplete(interaction, option) {
            const timerData = await TimerModel.findOne({
              "account.userID": interaction.user.id,
            });

            if (
              !timerData ||
              !timerData.currentSemester.semesterSubjects.length
            )
              return ["NA"];

            return timerData.currentSemester.semesterSubjects.map(
              (subject) => `${subject.subjectCode} - ${subject.subjectName}`
            );
          },
          async execute({ client, interaction }) {
            const chosenSubject = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("subject");

            if (chosenSubject === "NA")
              return interaction.reply({
                content:
                  "You don't have any subjects registered this semester.",
                flags: MessageFlags.Ephemeral,
              });

            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              if (!timers.timerData.sessionData.sessionStartTime)
                return interaction.reply({
                  content: "You don't have an active session.",
                  flags: MessageFlags.Ephemeral,
                });

              if (chosenSubject === timers.timerData.sessionData.sessionTopic)
                return interaction.reply({
                  content: `${chosenSubject} is already the active topic for this session.`,
                  flags: MessageFlags.Ephemeral,
                });

              const newTopicEmbed = new EmbedBuilder()
                .setTitle(`${Emojis.Verify} Success`)
                .setColor(ColorCodes.Default)
                .setDescription(
                  `Set '${chosenSubject}' as the new session topic`
                )
                .setTimestamp();

              timers.timerData.sessionData.sessionTopic = chosenSubject;

              let sessionTopic =
                timers.timerData.currentSemester.semesterSubjects.find(
                  (subject) =>
                    subject.subjectCode.trim().toLowerCase() ===
                    chosenSubject.split(" - ")[0].trim().toLowerCase()
                );

              if (
                sessionTopic &&
                !timers.timerData.sessionData.subjectsStudied.includes(
                  sessionTopic.subjectCode
                )
              ) {
                sessionTopic.timesStudied++;
                timers.timerData.sessionData.subjectsStudied.push(
                  sessionTopic.subjectCode
                );
              }

              await timers.timerData.save();

              return interaction.reply({
                embeds: [newTopicEmbed],
              });
            } catch (error) {
              return interaction.reply({
                content: `I ran into an error :(\n\n${error.message}`,
                flags: MessageFlags.Ephemeral,
              });
            }
          },
        },
        end: {
          description: "End the current Semester",
          async execute({ client, interaction }) {
            try {
              const timers = await Timers.create(
                interaction.user.id,
                false,
                false,
                interaction,
                client
              );

              const confirmationButtons =
                new ActionRowBuilder<ButtonBuilder>().addComponents([
                  new ButtonBuilder()
                    .setCustomId("endSemester")
                    .setStyle(ButtonStyle.Danger)
                    .setLabel("End Semester"),
                  new ButtonBuilder()
                    .setCustomId("stopEndSemester")
                    .setStyle(ButtonStyle.Secondary)
                    .setLabel("Don't End the Semester"),
                ]);

              const endEmbed = new EmbedBuilder()
                .setTitle(`⚠️ Confirmation Required ⚠️`)
                .setColor(ColorCodes.ErrorRed)
                .setDescription(
                  `Use the buttons below to confirm or deny ending the current semester, we recommend using timer stats before ending the semester.\n\nButtons are active for 5 minutes.`
                )
                .setFooter({
                  text: "Your season level will be converted to RP that will be added to your account rank if you reset.",
                })
                .setTimestamp();

              await interaction.reply({
                embeds: [endEmbed],
                components: [confirmationButtons],
                withResponse: true,
              });

              const filter = (i: Interaction) => {
                return i.user.id === interaction.user.id;
              };

              const collector =
                interaction.channel.createMessageComponentCollector({
                  filter,
                  time: 300000,
                  componentType: ComponentType.Button,
                });

              collector.on("collect", async (i) => {
                if (i.customId === "stopEndSemester") {
                  endEmbed.setDescription("Process aborted by user request.");

                  await interaction.editReply({
                    embeds: [endEmbed],
                    components: [],
                  });

                  return collector.stop("1407");
                } else if (i.customId === "endSemester") {
                  const semesterName =
                    timers.timerData.currentSemester.semesterName;
                  const endHelper = await timers.endSemester();

                  endEmbed
                    .setTitle(`${semesterName} Recap`)
                    .setColor(ColorCodes.Default)
                    .setDescription(endHelper)
                    .setFooter({
                      text: "Good luck on your next journey, we wish you the best!",
                    })
                    .setTimestamp();

                  await interaction.editReply({
                    embeds: [endEmbed],
                    components: [],
                  });

                  return collector.stop("1407");
                }
              });

              collector.on("end", (collected, reason) => {
                if (reason === "1407") return;

                endEmbed
                  .setTitle(`Process Aborted`)
                  .setDescription("Session Timed Out")
                  .setColor(ColorCodes.Default)
                  .setFooter({
                    text: "Re-run the command to use the buttons",
                  })
                  .setTimestamp();

                return interaction.editReply({
                  embeds: [endEmbed],
                  components: [],
                });
              });
            } catch (error) {
              return interaction.editReply({
                content: error,
              });
            }
          },
        },
      },
    });
  }
}
