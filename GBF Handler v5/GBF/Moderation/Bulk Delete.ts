import {
  ChannelType,
  Collection,
  CommandInteraction,
  GuildTextBasedChannel,
  Message,
  User,
} from "discord.js";
import { Emojis } from "../../Handler";

/**
 * Class to handle bulk deletion of messages in a Discord channel.
 */
export class GBFClearMessages {
  private readonly Action: Message | CommandInteraction;
  private readonly Amount: number;
  private readonly TargetUser: User;
  private readonly TargetChannel: GuildTextBasedChannel;
  private readonly CustomMessage: string;

  /**
   * Constructs a new GBFClearMessages instance.
   * @param {Message | CommandInteraction} Action The message or command interaction that triggered the clear action.
   * @param {number} Amount The number of messages to delete.
   * @param {GuildTextBasedChannel} TargetChannel The target text-based channel from which to delete messages.
   * @param {User} [TargetUser] The optional user whose messages to delete.
   * @param {string} [CustomMessage]  A custom message to send instead of the default one.
   */
  constructor(
    Action: Message | CommandInteraction,
    Amount: number,
    TargetChannel: GuildTextBasedChannel,
    TargetUser?: User,
    CustomMessage?: string
  ) {
    this.Action = Action;

    if (Amount <= 0) throw new Error(`You cannot delete 0 or fewer messages.`);
    else if (Amount > 100) {
      console.warn(
        `You cannot delete more than 100 messages at a time, automatically setting the amount to 100.`
      );
      this.Amount = 100;
    } else this.Amount = Amount;

    this.TargetUser = TargetUser;

    if (TargetChannel.type !== ChannelType.GuildText)
      throw new Error(`The channel must be a Text Based Channel.`);
    else this.TargetChannel = TargetChannel;

    if (CustomMessage.length > 1024)
      throw new Error(
        `The Custom Message specified in the clear command exceeds 1024 characters.`
      );
    this.CustomMessage = CustomMessage;
  }

  /**
   * Runs the bulk message deletion process.
   */
  public async RunClear() {
    try {
      const FilteredMessagesCollection: Collection<string, Message<true>> = (
        await this.TargetChannel.messages.fetch({
          limit: this.TargetUser ? 100 : this.Amount,
        })
      ).filter((message) => {
        if (this.TargetUser)
          return message.author === this.TargetUser && !message.pinned;
        else return true;
      });

      const FilteredMessages: Message<true>[] = [
        ...FilteredMessagesCollection.values(),
      ].slice(0, this.Amount);

      if (FilteredMessages.length > 0) {
        await this.TargetChannel.bulkDelete(FilteredMessages, true);

        let SuccessMessage = `${Emojis.Verify} I deleted \`${
          this.Amount
        }\` messages${
          this.TargetUser ? ` sent by ${this.TargetUser.username}!` : "!"
        }`;

        if (this.CustomMessage.length > 0) SuccessMessage = this.CustomMessage;

        await this.Action.reply({
          content: SuccessMessage,
        });

        setTimeout(
          () =>
            this.Action instanceof CommandInteraction
              ? this.Action.deleteReply()
              : this.Action.delete(),
          5000
        );
      } else throw new Error(`There are no messages to delete.`);
    } catch (DeleteError) {
      throw DeleteError;
    }
  }
}
