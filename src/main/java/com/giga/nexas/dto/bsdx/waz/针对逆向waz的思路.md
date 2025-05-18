首先非常感谢这位来自法兰西的超人@koukdw，没他在，我不知道仅靠自己得琢磨多久，起码帮助我节省了一年的时间，真的非常感谢。

以及记录下逆向时的思路，思路交流可以在issue2中看到，但为了今后可能有人，或者说如果自己忘了以后，能够快速回想起逆向的细节，故此追加此md。

因为本人做javaweb开发出身，对底层实现的细节、对c语言相关工程的了解都只是浅尝辄止，做逆向这也是头一次，所以部分描述和结论可能并不准确，但可以保证大体方向无误。

## 1.前言
waz的结构拥有复杂的嵌套，bsdx和bhe中的waz内的属性大体一致，以及闪钢使用的应该也是同款迭代引擎，一样存在.waz，区别大多仅在子类类型是否有追加。

大部分“数值”都是小端有符号整型（int），4字节，暂时没有发现大端的读取方式，少部分位置会仅读取1字节为byte做数值，也有读取2字节short，8字节为double的，但总体来说相当少；string类型为null-terminated编码，遇到字节0x00即为字符串终止。

ida中发现exe保留了RTTI，将工厂函数和typeTable逆向后，剩下的就是把整个读取文件的逻辑照搬，将.waz原封不动按照读取时的方式，将其逆向为java逻辑，只要能保证自己写的一整套parse逻辑能将游戏内所有的.waz全部正常读取，并能通过对象还原回源文件，则代表逆向成功。

说白了，纯纯体力活，每一个都不是那么复杂，但是加起来就要命，这是一个痛苦的过程，单纯敲代码就花了半个月。

## 2.waz的结构
这个.waz是什么呢？个人推测是**对象在游戏中的行为模式单元**。

尚且不清楚每个数值都对应什么意思，但能对应得上RTTI的部分基本都有字符串，可以做到推测**大体上这个行为单元是干什么的**，但是**具体是做什么的**还需要反复修改，测试并验证游戏行为后才能得知。

waz的结构如下（伪代码）：
```java
class Waz {
    List<Skill> skillList; // 由读出的个数决定size大小
}

class Skill {
    String skillName;
    String skillKey; // key
    List<SkillPhase> skillPhases; // 由读出的个数决定size大小
    List<SkillSuffix> SkillSuffixes; // 由读出的个数决定size大小
}

class SkillPhase {
    List<SkillUnit> skillUnits; // size = 72
}

class SkillSuffix {
    int param1;
    int param2;
}

class SkillUnit {
    List<CEvent> skillInfoObjects;
    List<SkillInfoUnknown> skillInfoUnknowns;
}

class CEvent {
    // 一共35种子类
}

class SkillInfoUnknown {
    FrameInfo frame;
    int param1;
    int param2;
    int param3;
}

class FrameInfo {
    int startFrame;
    int endFrame;
}
```
嗯……还是相当复杂的，工作量主要集中在CEvent子类的逆向上，部分RTTI的类还有嵌套，在此不一一列举，详情请看`dto/bsdx/waz/wazfactor/wazinfoclass/obj`。

read时的顺序遵循上述从上至下的顺序结构，游戏在读取时不会跳跃或退回指针。

在读任何一个单独的元素时，会首先读取一个flag，用于判断下一个块是否存在，不为0代表存在（实际上这种情况的flag只会等于1，暂时没有发现例外），反之不存在，不进行读取。

其中SkillPhase的命名说实话有些欠考虑，因为逆向后验证其行为时，发现并不代表“阶段”，而是普通的行为单元，游戏会通过各种判断，决定当前要应用哪个单元到游戏行为上。

当然，也可以直接看代码。