from dataclasses import dataclass
from typing import List


@dataclass
class CalculationRequest:
    manufacturer_kbm: float
    operator_kbm: float
    security_goals: List[str]
    required_goals: List[str]
    coverage_amount: float


@dataclass
class CalculationResult:
    calculated_cost: float
    risk_score: float

@dataclass
class Incident:
    incident_id: str
    order_id: str
    policy_id: str
    damage_amount: float


@dataclass
class IncidentRequest:
    incident: Incident
    coverage_amount: float


@dataclass
class IncidentResult:
    payment_amount: float
    is_fraud: bool
    message: str


@dataclass
class IncidentRecord:
    damage_amount: float


@dataclass
class KbmRequest:
    manufacturer_kbm: float
    operator_kbm: float
    manufacturer_history: List[IncidentRecord]
    operator_history: List[IncidentRecord]


@dataclass
class KbmResult:
    new_manufacturer_kbm: float
    new_operator_kbm: float