using UnityEngine;
using System.Collections;


namespace TechTweaking.Bluetooth
{



	public class BtPackets {
		public readonly byte[] Buffer;
		public  readonly int Count;
		private readonly int[] packet_indices;
		
	

		internal BtPackets(byte[] buffer ){
			this.Buffer = buffer;

			if(buffer.Length>0){
				this.Count = System.BitConverter.ToInt32(this.Buffer,0)  +1;

				//A list that doesn't contain the first index of the first packet, because it's already known
				this.packet_indices = new int[this.Count];
				
				this.packet_indices[0] = this.Count*4;

				for(int i=1;i<this.Count; i+= 1){//the first 4 bytes are the list size
					int packetSize = System.BitConverter.ToInt32(buffer,i*4);
					this.packet_indices[i] = packet_indices[i - 1] + packetSize ;
				}
			}
		}

		public int get_packet_offset_index (int indx) {
			return this.packet_indices[indx];
		}

		public int get_packet_size (int indx) {
			int excludedIndx = this.Count == (indx + 1) ? this.Buffer.Length : this.packet_indices[indx +1];
			return excludedIndx - this.packet_indices[indx];
		}


	}
}