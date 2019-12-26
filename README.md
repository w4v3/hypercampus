# HyperCampus
source and documentation of the HyperCampus learning app from Google Play store

## What is HyperCampus?

HyperCampus is a pun on the word _hippocampus_, which describes [a certain brain structure](https://en.wikipedia.org/wiki/Hippocampus) that plays a major role in long-term memory and also happens to be the Ancient Greek word for _seahorse_. In relevant terms, it is an app that helps you create flashcard decks for studying whatever you want and that schedules the reviews of those flashcards intelligently for you. To achieve that, it uses a [_spaced repetition algorithm_](https://en.wikipedia.org/wiki/Spaced_repetition) combining machine learning with insights from psychological and neurobiological research. HyperCampus adapts to your memory and tries to predict the optimal time for reviewing each flashcard. See [How does it work?](#how-does-it-work) for details.

### How is HyperCampus different from Anki and SuperMemo?

Maybe you are using spaced repetition software like Anki or SuperMemo already and you're wondering: "Why should I use HyperCampus?" If you are new to spaced repetition, you might want to skip this section.

Well, the answer is: Maybe you shouldn't. There is not yet an ultimate truth in spaced repetition research, and every tool has its advantages and disadvantages. If you feel comfortable with your current system and have the learning success you are aiming at, there is no reason to change anything. Let me compare HyperCampus to two giants in spaced repetition, SuperMemo and Anki.

The [SuperMemo](https://www.supermemo.com/) algorithms are developed by Piotr Woźniak, who claims to be the father of spaced repetition. While it is possible to argue about that, as a matter of fact, a lot of free spaced repetition software today is based on the only open source version of the SuperMemo algorithm, SM-2 from 1987. Newer versions of the algorithm are fairly sophisticated (see, e.g., [SM-17](https://supermemo.guru/wiki/Algorithm_SM-17)) and use insights gained from data collected with SuperMemo itself over the last decades. HyperCampus cannot keep up with this empirical advantage since its performance hasn't been thoroughly tested yet. Also, SuperMemo comes with an extensive software suite for creating the flashcard collections, where HyperCampus only provides basic flashcard creation tools. HyperCampus is, however, open-source and completely free, as opposed to the $66 you have to pay for the latest SuperMemo software.

[Anki](https://apps.ankiweb.net/) is also a free (except for iPhone) spaced repetition tool that is available on many different platforms. It is based on the open-source version of the SuperMemo algorithm, with some modifications. Anki is extremely flexible, allowing for a high level customization of the flashcards and providing a large number of add-ons and pre-made decks by other users. This is a major advantage over HyperCampus; however, this is not only due to my laziness, but part of the design philosophy of HyperCampus: Flashcards should be as simple as possible, as spaced repetition works best with _atomic_ memories, simple questions with simple answers. The strength of HyperCampus lies in the algorithm design, which draws from empirical research and adapts more flexibly to the learner and the learning content. Furthermore, you will be able to use your anki decks with HyperCampus soon.

## How to use it?

When you open the HyperCampus app, you should see the (initially empty) list of your _courses_, which are collections of cards that deal with the same topic. You can choose an icon (for example an emoji, or just a letter), a name and how many new cards you would like to study per day. I would recommend to study between 5 and 20 new cards for each course per day, but note that obviously you have to add some cards first before you can study any.

Courses consist of _lessons_, and the lessons contain your cards. New cards are added to your studies in the order that they are displayed in the app, progressing from the first to the last lesson. For each card, you can specify the contents of the "question" side and the "answer" side.

You can enter your daily review by pressing the floating button with the HyperCampus icon. First, new cards are displayed to you for studying. Then, cards due for review are shown and you should try to recall the answers on them. When you think you know the answer, or decide that you don't know it any more, reveal the answer side and grade your performance using the scale from purple (completely forgotten) to yellow (very easy). Try to assess nuances in your performance. For example, if you knew it, but it took you a long time, tap a bit right from the bittle. If you forgot it, but it was on the tip of your tongue, tap a bit left from the middle.

Apart from a complete review, you can choose to study a course or a lesson in isolation by tapping on the button indicating how many cards are due in the respective course or lesson. Note that new cards are only displayed when reviewing a course (i.e., not for a specific lesson). You should review all due cards every day as due cards will pile up when you don't review them, but the algorithm is adapted to work even if you review a card later than you should.

In the settings, you can choose which spaced repetition algorithm to use. Of course I recommend the native HyperCampus algorithm, but as it is still experimental, I will allow you to use [SM-2](#the-supermemo-2-algorithm), on which Anki is based, as well. Note that when changing the algorithm, it will take some time to adapt it to your data, depending on how long you have used the other algorithm. For this reason, changing the algorithm frequently is not encouraged.

You can also set the _forgetting index_, which determines how much of the content learnt you want to keep in memory during any time. The default is 10%, meaning that the algorithms schedule cards such that you should be able to remember around 90% of the contents. Setting it higher is not recommended as it increases the workload without much benefit. If you are happy with a lower retention rate, let's say remembering around 80%, you can set the forgetting index to 20% to reduce workload. In fact, a lower retention rate leads to a much more effective acquisition; but this effect only works up to a forgetting index of 60%, after which the probability of recall is too low to allow memories to be acquired.

### Features released in the near future
- [ ] import/export of flashcard collections (including Anki files)
- [ ] cram mode
- [ ] customize the review process and how new cards are learnt
- [ ] inspect your learning statistics

## How does it work?

HyperCampus implements a _spaced repetition system_, which is often considered to be the most efficient way to put facts into long-term memory. Spaced repetition relies on the [https://en.wikipedia.org/wiki/Spacing_effect](Spacing effect), stating that learning increases when study sessions are spaced out rather than massed. HyperCampus uses a computational model of long-term memory to predict the point in time when the probabiliy of forgetting an item rises above the requested _forgetting index_ (10% by default). Most of the algorithm is then concerned with improving the memory model in order to make more accurate predictions. Before diving into the details, let me explain some general concepts of spaced repetition by looking at how the SuperMemo-2 algorithm is implemented in HyperCampus.

### The SuperMemo 2 Algorithm

A detailed description can be found [here](https://supermemo.guru/wiki/SuperMemo_1.0_for_DOS_(1987)#Algorithm_SM-2). The algorithm has two fixed intervals: At first, a card is shown after 1 day, and then 6 days later. Each time an item is successfully reviewed, the interval until it is shown the next time increases by a certain factor. This rests on the assumption that successfully reviewing a memory strengthens it in a way such that it takes longer to forget it. In case of an unsuccessful review, i.e. if the item was forgotten, the interval is reset to 1 day.

The algorithm can also account for differences in learning difficulty across items by associating each item with an _easiness factor_ EF. The EF is the factor by which the intervals are increased after successful reviews. After each review, the EF of an item is updated depending on the grade the user has submitted. The implementation of SM-2 in HyperCampus differs from the original in that the original uses a grading system with six categories, from 0 to 5, while grading in HyperCampus is continuous. For SM-2, the continuous grade is scaled linearly to fit into the interval from 0 to 5, and then the update of the EF is carried out using a decimal grade. 

### The HyperCampus 1 Algorithm

The discussion involves a lot of maths and is thus moved to [this document](/model.ipynb).

## Release notes

### 20191223: version 0.alpha
- implemented SM 2 algorithm
- several stress tests passed
### 20191222: version 0.0
- basic app functionality implemented

## Licences

The app includes the SuperMemo-2 algorithm, with modifications detailed [above](#the-supermemo-2-algorithm).
```
Algorithm SM-2, (C) Copyright SuperMemo World, 1991.
```
* https://www.supermemo.com
* https://www.supermemo.eu

It also uses the [MaterialShowcaseView](https://github.com/deano2390/MaterialShowcaseView) library by Dean Wild, licensed under Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0).

<img align="right" src="/logo.svg"/>
