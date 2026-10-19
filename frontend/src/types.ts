export type StatutTicket = "OUVERT" | "EN_COURS" | "FERME";

export interface Ticket {
  id: number;
  titre: string;
  description: string | null;
  statut: StatutTicket;
  dateCreation: string;
}

export interface CorpsCreationTicket {
  titre: string;
  description?: string;
}

export interface ErreurApi {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
