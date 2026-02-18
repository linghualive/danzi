import { roomLifecycleActions } from './room/roomLifecycleActions.js';
import { roomPlayerActions } from './room/roomPlayerActions.js';
import { roomChatActions } from './room/roomChatActions.js';
import { roomProductActions } from './room/roomProductActions.js';

export const roomActions = {
    ...roomLifecycleActions,
    ...roomPlayerActions,
    ...roomChatActions,
    ...roomProductActions
};
