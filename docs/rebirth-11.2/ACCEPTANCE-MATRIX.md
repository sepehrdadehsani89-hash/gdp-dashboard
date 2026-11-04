# 11.2 device acceptance matrix

All cases below are **NOT TESTED** as Android runtime acceptance in this upgrade. Host policy/fixture tests cover selected algorithms only; they do not close device cases. No physical Redmi9, real Google mailbox, authorized Fish session or accessory is connected here. The preserved API29 emulator is not a working substitute for these tests.

## Numbered master cases

| # | Required case | Runtime status |
| --- | --- | --- |
| 1 | English wake phrase | NOT TESTED |
| 2 | Spanish wake phrase | NOT TESTED |
| 3 | Quiet-room recognition | NOT TESTED |
| 4 | Television noise | NOT TESTED |
| 5 | Music noise | NOT TESTED |
| 6 | Multiple speakers | NOT TESTED |
| 7 | Owner speech | NOT TESTED |
| 8 | Guest speech | NOT TESTED |
| 9 | Unknown speaker | NOT TESTED |
| 10 | Soft speaking | NOT TESTED |
| 11 | Long utterances | NOT TESTED |
| 12 | Short commands | NOT TESTED |
| 13 | Natural pauses | NOT TESTED |
| 14 | Interrupted speech | NOT TESTED |
| 15 | Repeated wake phrase | NOT TESTED |
| 16 | Bluetooth microphone | NOT TESTED |
| 17 | Recognizer busy | NOT TESTED |
| 18 | No speech match | NOT TESTED |
| 19 | Network recognition failure | NOT TESTED |
| 20 | Microphone permission removal | NOT TESTED |
| 21 | Cloud versus Claude | NOT TESTED |
| 22 | WhatsApp versus what’s up | NOT TESTED |
| 23 | Weather versus whether | NOT TESTED |
| 24 | Gmail versus email | NOT TESTED |
| 25 | Home versus home screen | NOT TESTED |
| 26 | Installed app pronunciation | NOT TESTED |
| 27 | Contact aliases | NOT TESTED |
| 28 | Saved destination aliases | NOT TESTED |
| 29 | Spanish application names | NOT TESTED |
| 30 | User corrections | NOT TESTED |
| 31 | Multiple candidate applications | NOT TESTED |
| 32 | Sensitive-number protection | NOT TESTED |
| 33 | One short conversation | NOT TESTED |
| 34 | One long conversation | NOT TESTED |
| 35 | Fifty consecutive simple commands | NOT TESTED |
| 36 | Repeated Fish Audio requests | NOT TESTED |
| 37 | Rapid interruption | NOT TESTED |
| 38 | Repeated app launching | NOT TESTED |
| 39 | Wallpaper appearance and disappearance | NOT TESTED |
| 40 | Overlay creation and removal | NOT TESTED |
| 41 | Account connection and disconnection | NOT TESTED |
| 42 | Large document import | NOT TESTED |
| 43 | Photo selection | NOT TESTED |
| 44 | Low-memory conditions | NOT TESTED |
| 45 | App backgrounding | NOT TESTED |
| 46 | Screen locking | NOT TESTED |
| 47 | Screen-off wake behavior when supported | NOT TESTED |
| 48 | Network interruption | NOT TESTED |
| 49 | Recognizer service failure | NOT TESTED |
| 50 | Android process recreation | NOT TESTED |

## Apps, every saved alias, foreground and restricted background

| Required case | Runtime status |
| --- | --- |
| Spotify | NOT TESTED |
| Instagram | NOT TESTED |
| YouTube | NOT TESTED |
| Gmail | NOT TESTED |
| Google Maps | NOT TESTED |
| Gallery | NOT TESTED |
| Camera | NOT TESTED |
| Clock | NOT TESTED |
| Settings | NOT TESTED |
| JARVIS itself | NOT TESTED |
| Every configured application alias | NOT TESTED |

## Gmail and personal content

| Required case | Runtime status |
| --- | --- |
| No Google account connected | NOT TESTED |
| Authorized Gmail read-only access | NOT TESTED |
| Latest email | NOT TESTED |
| Latest unread email | NOT TESTED |
| Search by sender | NOT TESTED |
| Read selected message | NOT TESTED |
| Summarize selected message | NOT TESTED |
| Multiple accounts | NOT TESTED |
| Revoked authorization | NOT TESTED |
| Network failure | NOT TESTED |
| No messages found | NOT TESTED |
| File picker | NOT TESTED |
| Folder permission | NOT TESTED |
| Photo Picker | NOT TESTED |
| Large file | NOT TESTED |
| Unsupported file | NOT TESTED |
| Permission revocation | NOT TESTED |

## Live-only conversations and preserved configuration

| Required case | Runtime status |
| --- | --- |
| Captions appear while speaking | NOT TESTED |
| Partial words update | NOT TESTED |
| Final corrections replace provisional text | NOT TESTED |
| JARVIS answers display correctly | NOT TESTED |
| The wallpaper uses temporary state | NOT TESTED |
| Phantom uses the same temporary state | NOT TESTED |
| The main app uses the same temporary state | NOT TESTED |
| Sessions clear when they end | NOT TESTED |
| Chat messages are not added to a permanent database | NOT TESTED |
| Explicitly saved settings remain intact | NOT TESTED |
| Previously saved chats can be deleted separately | NOT TESTED |

## Actual performance measurements

| Required case | Runtime status |
| --- | --- |
| Wake recognition latency | NOT TESTED |
| First partial transcript latency | NOT TESTED |
| Utterance finalization time | NOT TESTED |
| Autocorrection latency | NOT TESTED |
| Intent classification time | NOT TESTED |
| App launch dispatch time | NOT TESTED |
| Tool result time | NOT TESTED |
| Local model loading time | NOT TESTED |
| Time to first generated token | NOT TESTED |
| Fish Audio synthesis startup | NOT TESTED |
| Time to first spoken audio | NOT TESTED |
| Interruption latency | NOT TESTED |
| Memory usage over long sessions | NOT TESTED |
| Crash frequency | NOT TESTED |
| ANR frequency | NOT TESTED |
| Emulator measurements | NOT TESTED |
| Development device measurements | NOT TESTED |
| Redmi 9 physical-device measurements | NOT TESTED |

Record observed result, OS/device, compatible signing/version, permissions/settings, reproduction steps and content-free timing/error evidence for each future run. Mail or account “success” must be compared with the actual authorized mailbox. No invented account data or synthetic timing closes a case. No uninstall/data-clear instruction is part of these tests.
