export enum EventState {
    CREATED = 'CREATED',
    UPDATED = 'UPDATED',
    DELETED = 'DELETED',
    TAG_ADD = 'TAG_ADD',
    TAG_REMOVED = 'TAG_REMOVED',
    PROP_ADD = 'PROP_ADD',
    PROP_REMOVED = 'PROP_REMOVED',
    REVERTED = 'REVERTED',
    DUPLICATED = 'DUPLICATED',
    MERGE_TAG_ADD = 'MERGE_TAG_ADD',
    MERGE_PROP_ADD = 'MERGE_PROP_ADD',
    MERGE_UPDATED = 'MERGE_UPDATED'
}

export interface Event {
    id: string;
    user: string;
    contact: string;
    eventState: EventState;
    propKey: string;
    prevState: string;
    currentState: string;
    eventTime: Date;
}