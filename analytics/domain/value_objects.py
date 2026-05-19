from dataclasses import dataclass


@dataclass(frozen=True)
class Money:
    amount: float

    def min(self, other: "Money") -> "Money":
        return Money(min(self.amount, other.amount))


@dataclass(frozen=True)
class Kbm:
    value: float

    def apply_factor(self, factor: float) -> "Kbm":
        return Kbm(self.value * factor)