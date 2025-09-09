import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  EmbedBuilder,
} from "discord.js";
import { SlashCommand, GBF, ColorCodes } from "../../Handler";

import { Unsplash_API_Key } from "../../Config/GBF Config.json";

export class Animals extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "animals",
      description: "Get pictures of cute animals!",
      category: "Animals",
      cooldown: 5,
      subcommands: {
        bird: {
          description: "A cute bird picture to brighten your day! 🌹🐦",
          async execute({ client, interaction }) {
            try {
              const res = await fetch(
                "https://api.unsplash.com/photos/random?query=bird&count=1",
                {
                  headers: {
                    Authorization: `Client-ID ${Unsplash_API_Key}`,
                  },
                }
              );

              if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

              const data = await res.json();
              const image = data[0].urls.regular;

              const birdImage = new EmbedBuilder()
                .setTitle("Birds of a Feather 🐤")
                .setImage(image)
                .setColor(ColorCodes.Default)
                .setTimestamp();

              const birdImageButton =
                new ActionRowBuilder<ButtonBuilder>().addComponents(
                  new ButtonBuilder()
                    .setURL(image)
                    .setLabel("View Full Image 🖼️")
                    .setStyle(ButtonStyle.Link)
                );

              return interaction.reply({
                embeds: [birdImage],
                components: [birdImageButton],
              });
            } catch (error) {
              console.error(error);
              return interaction.reply({
                content: `Oh no! It looks like I ran into an error :(\n\nError:\n\`\`\`js\n${error}\`\`\``,
                ephemeral: true,
              });
            }
          },
        },
        dog: {
          description: "Doggo 🐶",
          async execute({ client, interaction }) {
            try {
              const res = await fetch(
                "https://dog.ceo/api/breeds/image/random"
              );

              if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

              const image = (await res.json()).message;

              const dogImageEmbed = new EmbedBuilder()
                .setTitle("Doggy Delight 🐕")
                .setImage(image)
                .setColor(ColorCodes.Default)
                .setTimestamp();

              const dogImageButton =
                new ActionRowBuilder<ButtonBuilder>().addComponents(
                  new ButtonBuilder()
                    .setURL(image)
                    .setLabel("View Full Image 🖼️")
                    .setStyle(ButtonStyle.Link)
                );

              return interaction.reply({
                embeds: [dogImageEmbed],
                components: [dogImageButton],
              });
            } catch (error) {
              console.error(error);
              return interaction.reply({
                content: `Oh no! It looks like I ran into an error :(\n\nError:\n\`\`\`js\n${error}\`\`\``,
                ephemeral: true,
              });
            }
          },
        },
        duck: {
          description: "Quack 🦆",
          async execute({ client, interaction }) {
            try {
              const res = await fetch("https://random-d.uk/api/v2/random");

              if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

              const image = (await res.json()).message;

              const duckImageEmbed = new EmbedBuilder()
                .setTitle("Quack 🦆")
                .setImage(image)
                .setColor(ColorCodes.Default)
                .setTimestamp();

              const duckImageButton =
                new ActionRowBuilder<ButtonBuilder>().addComponents(
                  new ButtonBuilder()
                    .setURL(image)
                    .setLabel("View Full Image 🖼️")
                    .setStyle(ButtonStyle.Link)
                );

              return interaction.reply({
                embeds: [duckImageEmbed],
                components: [duckImageButton],
              });
            } catch (error) {
              console.error(error);
              return interaction.reply({
                content: `Oh no! It looks like I ran into an error :(\n\nError:\n\`\`\`js\n${error}\`\`\``,
                ephemeral: true,
              });
            }
          },
        },
        cat: {
          description: "A cute cat picture to brighten your day! 🐱",
          async execute({ client, interaction }) {
            try {
              const res = await fetch(
                "https://api.unsplash.com/photos/random?query=cat&count=1",
                {
                  headers: {
                    Authorization: `Client-ID ${Unsplash_API_Key}`,
                  },
                }
              );

              if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

              const data = await res.json();
              const image = data[0].urls.regular;

              const catImage = new EmbedBuilder()
                .setTitle("Kitty 🐱")
                .setImage(image)
                .setColor(ColorCodes.Default)
                .setTimestamp();

              const catImageButton =
                new ActionRowBuilder<ButtonBuilder>().addComponents(
                  new ButtonBuilder()
                    .setURL(image)
                    .setLabel("View Full Image 🖼️")
                    .setStyle(ButtonStyle.Link)
                );

              return interaction.reply({
                embeds: [catImage],
                components: [catImageButton],
              });
            } catch (error) {
              console.error(error);
              return interaction.reply({
                content: `Oh no! It looks like I ran into an error :(\n\nError:\n\`\`\`js\n${error}\`\`\``,
                ephemeral: true,
              });
            }
          },
        },
      },
    });
  }
}
