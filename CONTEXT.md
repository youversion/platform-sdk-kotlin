# YouVersion Platform SDK

The vocabulary this SDK exposes to the apps that embed it. Terms here are the ones a consumer
meets in the public API and in our documentation, and they are shared with the Swift, React, and
React Native SDKs — a term defined here should mean the same thing on every platform.

## Language

### Search

**Suggested query**:
A query offered to a reader while they type, derived from what they have entered so far.
_Avoid_: autocomplete, typeahead, completion

**Trending query**:
A query many readers are running right now, independent of anything the current reader has typed.
_Avoid_: popular search, top search

**User intent**:
What a reader appears to be looking for — a Bible reference, scripture text, or a topic. Open-ended:
the platform may name an intent this SDK has never heard of, and an unrecognized one is carried
through rather than treated as an error.
_Avoid_: search type, search mode, category

**Did you mean**:
Alternative spellings the platform offers alongside results for the query as the reader wrote it.
The results belong to what was asked; these are merely offered.
_Avoid_: suggestion, correction, spelling suggestion

**Search instead for**:
The reader's original wording, returned when the platform judged it a mistake, silently corrected it,
and returned results for the correction. The inverse of _Did you mean_: here the results belong to
the correction, and the original is what is being offered.

**Unified search**:
A single search returning both verses and topics together, rather than either kind alone.
_Avoid_: combined search, search results, everything search

**Result kind**:
A category of search result — verses or topics — that a unified search can be narrowed to.
Distinct from the sparse selection of properties the rest of this SDK calls _fields_, although the
platform happens to spell both the same way. Narrowing result kinds asks for fewer kinds of thing;
selecting fields asks for less of each thing.

**Topic**:
A named subject a reader can search for and that scripture speaks to, such as anxiety or forgiveness.
A topic may name related **subtopics**.

### Cross-cutting

**Bible reference**:
An address in scripture — a version, a book, a chapter, and optionally a verse or range of verses.
Note that _reference_ alone is ambiguous near search, where it is also the name of a user intent
meaning "this reader is looking for a specific address rather than for text or a topic."

**Language range**:
A reader's language preference, written as a BCP 47 language tag such as `en-US`, or as `*` to mean
any language. Several may be given, and their order is their order of preference.
_Avoid_: locale, language code, language filter
