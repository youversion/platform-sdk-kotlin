# YouVersion Platform SDK

The vocabulary this SDK exposes to the apps that embed it. Terms here are the ones a consumer meets
in the public API and in our documentation. Where a term also appears in the Swift, React, or React
Native SDKs it is meant to carry the same meaning; where this SDK genuinely means something
different, the entry says so.

A term earns an entry by being contestable — a word with two plausible readings, or one this SDK
uses differently from everyday usage. Terms whose meaning is obvious are left to the KDoc.

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

### Bibles

**Bible version**:
A translation or edition of scripture, identified by a numeric id. This SDK names that id
`versionId` everywhere except search, which takes the same value as a `bibleId` — the platform's own
spelling in that one place.
_Avoid_: bible, translation id

**Permitted version**:
A Bible version an embedding app is allowed to use — one that satisfies every allow and deny list the
integrator sets at configuration. Permission here is the integrator's policy, not the reader's
preference.
_Avoid_: available version, allowed version, filtered version

**Passage**:
Two things wear this name. A _passage id_ is an address within a version's text, such as `JHN.3.16`
or `GEN.INTRO`. A `BiblePassage` is the scripture returned for one — the text, not the address.
_Avoid_: passage as a synonym for _Bible reference_

**Block**:
A unit of laid-out scripture — one run of text with its own indents, margins and alignment, or a
table. A block is neither a verse nor a paragraph of the source: it may span several verses, begin
partway through one, or display none at all. Because of that a block names only the _first verse_ it
displays rather than a whole _Bible reference_.
_Avoid_: paragraph, verse, section, chunk

**Focused reference**:
The one reference a reader is being pointed at — arrived at by following a search result or a link —
drawn at full strength while the rest of the chapter dims around it. Distinct from a reader's
_selection_: a selection is something the reader made and can act on, and it lasts until they undo
it, whereas a focus is something the SDK did *to* the reader and any deliberate scroll or tap
dismisses it. A focus always names a verse inside the chapter on display; a whole chapter cannot be
focused.
_Avoid_: selected verse, highlighted verse, active verse

**Downloaded**:
Held on the device for offline reading because someone asked for it. Distinct from _cached_, which is
incidental and may be discarded at any time; only downloads live in the SDK's persistent store.
_Avoid_: saved, offline copy, legacy cache

**Book title**:
A title naming the book itself — a major title (`mt`) or introduction title (`imt`) — as opposed to a
section heading naming a part of it. Carried on rendered text as `BibleTextCategory.BOOK_TITLE`, and
load-bearing rather than cosmetic: a host uses it to suppress a heading of its own that would
otherwise duplicate the passage's. The Swift SDK does not draw this distinction and calls both a header.
_Avoid_: header, heading, major title

### Highlights

**Highlight**:
A reader's color marking on scripture, owned by their YouVersion account. Two public types spell it:
`BibleHighlight` is the domain shape — a _Bible reference_ and a color — while `Highlight` is the
platform's wire shape, addressed by `bibleId` and `passageId`.
_Avoid_: annotation, marker, bookmark

**Pending highlight**:
A highlight the reader has made that the platform has not yet accepted. It is bound to the account
that made it, so it outlives a sign-out rather than passing to whoever signs in next.
_Avoid_: unsaved highlight, queued highlight, optimistic highlight

### Identity

**App key**:
The credential identifying an embedding app to the platform. It belongs to the app, not to a reader,
and never stands in for a reader's access token.
_Avoid_: API key, developer key, client id

**Data exchange**:
The flow by which an already signed-in reader grants a permission they did not grant at sign-in.
Named for the exchange of data it authorizes, not for any transfer of files.
_Avoid_: incremental consent, re-authorization, permission upgrade

**Granted permission**:
A permission a reader has given this app, such as `HIGHLIGHTS`. A grant is permanent from the app's
side — there is no revoking one — so a permission is either not yet granted or granted for good.
_Avoid_: scope, consent, entitlement

### Cross-cutting

**Bible reference**:
An address in scripture — a version, a book, a chapter, and optionally a verse or range of verses.
Note that _reference_ alone is ambiguous near search, where it is also the name of a user intent
meaning "this reader is looking for a specific address rather than for text or a topic."

**Language range**:
A reader's language preference, written as a BCP 47 language tag such as `en-US`, or as `*` to mean
any language. Several may be given, and their order is their order of preference.
_Avoid_: locale, language code, language filter

**Fields**:
A sparse selection of properties, naming which parts of a thing the platform should return. Distinct
from a _result kind_, which narrows what kinds of thing come back at all, although the platform
happens to spell both the same way.
_Avoid_: projection, sparse fieldset, columns
